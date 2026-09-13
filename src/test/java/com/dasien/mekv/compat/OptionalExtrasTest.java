package com.dasien.mekv.compat;

import com.dasien.mekv.factory.FactoryTier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipFile;

/** Runs on separate JVM classpaths with and without the optional dependency. */
public final class OptionalExtrasTest {
    private static int checks;
    private static final Path RESOURCES = Path.of("src/main/resources");

    public static void main(String[] args) throws Exception {
        boolean extras = Boolean.parseBoolean(args[0]);
        ClassLoader loader = OptionalExtrasTest.class.getClassLoader();
        boolean present;
        try {
            Class.forName("com.jerry.mekanism_extras.api.tier.AdvancedTier", false, loader);
            present = true;
        } catch (ClassNotFoundException absent) {
            present = false;
        }
        require(present == extras, "test JVM has the requested Extras classpath");
        require(FactoryTier.availableValues(extras).length == (extras ? 8 : 4), "available tiers");
        require(FactoryTier.ULTIMATE.next(extras) == (extras ? FactoryTier.ABSOLUTE : null), "ultimate upgrade boundary");
        require(FactoryTier.INFINITE.next(extras) == null, "last tier boundary");
        require(FactoryTier.BASIC.next(extras) == FactoryTier.ADVANCED, "base upgrade unaffected");
        // Reflecting common method signatures must never resolve absent Extras classes.
        for (String type : new String[]{"util.FactoryInteraction", "block.VillagerFactoryBlock",
                "item.FactoryBlockItem", "compat.ExtrasSupport", "mixin.ExtraItemTierInstallerMixin"}) {
            Class<?> clazz = Class.forName("com.dasien.mekv." + type, false, loader);
            clazz.getDeclaredMethods();
            clazz.getDeclaredConstructors();
            checks++;
        }
        // No always-loaded bytecode may name an Extras class in its type references.
        try (var classes = Files.walk(Path.of("build/classes/java/main/com/dasien/mekv"))) {
            for (Path file : classes.filter(p -> p.toString().endsWith(".class")).toList()) {
                if (file.getFileName().toString().startsWith("ExtrasIntegration")) {
                    continue;
                }
                String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                require(!bytes.contains("com/jerry/mekanism_extras"), "isolated optional type reference: " + file);
            }
        }
        Set<String> registered = new HashSet<>();
        for (FactoryTier tier : FactoryTier.availableValues(extras)) {
            for (String type : new String[]{"trader", "farmer", "iron_golem"}) {
                registered.add("mekv:" + tier.getSerializedName() + "_" + type + "_factory");
            }
        }
        require(registered.size() == (extras ? 24 : 12), "factory registration count");
        for (String kind : new String[]{"blocks", "items"}) {
            Set<String> tags = new HashSet<>();
            for (String prefix : extras ? new String[]{"", "extras/"} : new String[]{""}) {
                var tag = json(RESOURCES.resolve(prefix + "data/mekv/tags/" + kind + "/factories.json"));
                require(!tag.get("replace").getAsBoolean(), "extra tags append to base tags");
                tag.getAsJsonArray("values").forEach(value -> tags.add(value.getAsString()));
            }
            require(tags.equals(registered), "tags only reference registered factories");
        }
        for (FactoryTier tier : FactoryTier.values()) {
            int first = tier.processX(0);
            int last = tier.processX(tier.processes() - 1);
            int villager = tier.centeredSlotX(0, 2);
            int seed = tier.centeredSlotX(1, 2);
            require(villager + seed == first + last, "farmer header pair centered: " + tier);
            require(seed - villager == 18, "farmer header slots do not overlap");
            require(tier.centeredSlotX(0, 1) * 2 == first + last, "iron factory header unchanged");
            require(villager >= first && seed <= last, "header stays inside process grid");
            for (String type : new String[]{"trader", "farmer", "iron_golem"}) {
                String id = tier.getSerializedName() + "_" + type + "_factory";
                String prefix = tier.isExtra() ? "extras/" : "";
                var recipe = json(RESOURCES.resolve(prefix + "data/mekv/recipes/" + id + ".json"));
                require(recipe.getAsJsonObject("result").get("item").getAsString().equals("mekv:" + id), "recipe output");
                var loot = json(RESOURCES.resolve(prefix + "data/mekv/loot_tables/blocks/" + id + ".json"));
                require(loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries")
                        .get(0).getAsJsonObject().get("name").getAsString().equals("mekv:" + id), "loot output");
                if (tier.isExtra()) {
                    require(!Files.exists(RESOURCES.resolve("data/mekv/recipes/" + id + ".json")), "no unconditional extra recipe");
                    require(!Files.exists(RESOURCES.resolve("data/mekv/loot_tables/blocks/" + id + ".json")), "no unconditional extra loot");
                }
            }
        }
        require(json(RESOURCES.resolve("extras/pack.mcmeta")).getAsJsonObject("pack").get("pack_format").getAsInt() == 15, "1.20.1 data pack format");
        try (ZipFile zip = new ZipFile("libs/mekanism_extras-1.20.1-1.5.0.jar")) {
            byte[] led;
            try (var stream = zip.getInputStream(zip.getEntry("assets/mekanism_extras/textures/block/factory/led.png"))) {
                led = stream.readAllBytes();
            }
            require(Arrays.equals(led, Files.readAllBytes(RESOURCES.resolve("assets/mekv/textures/block/factory/extras_led.png"))), "Extras LED image matches dependency");
            require(!Arrays.equals(led, Files.readAllBytes(RESOURCES.resolve("assets/mekv/textures/block/factory/led.png"))), "base LED not reused");
            for (String tier : new String[]{"absolute", "supreme"}) {
                for (String suffix : new String[]{"", "_active"}) {
                    String entry = "assets/mekanism_extras/models/block/factory/front_led/" + (suffix.isEmpty() ? "" : "active/") + tier + ".json";
                    JsonObject original;
                    try (var stream = zip.getInputStream(zip.getEntry(entry))) {
                        original = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                    }
                    for (String type : new String[]{"trader", "farmer", "iron_golem"}) {
                        var model = json(RESOURCES.resolve("assets/mekv/models/block/" + tier + "_" + type + "_factory" + suffix + ".json"));
                        require(model.getAsJsonObject("textures").get("led").getAsString().equals("mekv:block/factory/extras_led"), "correct LED image");
                        boolean found = false;
                        for (var element : model.getAsJsonArray("elements")) {
                            var obj = element.getAsJsonObject();
                            if (obj.has("name") && obj.get("name").getAsString().equals("front_panel_led")) {
                                require(obj.get("faces").equals(original.getAsJsonArray("elements").get(0).getAsJsonObject().get("faces")), "LED UV matches Extras tier");
                                found = true;
                            }
                        }
                        require(found, "LED geometry exists");
                    }
                }
            }
        }
        System.out.println("Optional Extras (" + extras + "): " + checks + " checks passed.");
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
