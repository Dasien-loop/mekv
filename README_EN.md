# Mekv

[简体中文](README.md) | **English**

**Bring villager trading, farming, and iron golem production into your factory.**

Mekv combines Easy Villagers gameplay with Mekanism-style industrial automation. Insert a villager, supply energy, and configure a trade or production task. Independent processing lanes, parallel output, and item transport turn these activities into production lines that grow with your factory.

## Versions and Branches

| Branch | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `main` (this branch) | 1.20.1 | Forge 47.x | 17 |
| `1.21.1neoforge` | 1.21.1 | NeoForge 21.1.x | 21 |

Choose the file for your Minecraft version and loader. The two builds are not interchangeable. Configuration defaults, exact lane counts, and build checks below describe `main`; consult the other branch and its release files for its implementation details.

## Features

| Factory | Purpose | Requirements |
| --- | --- | --- |
| Trading Factory | Performs villager trades with independent inputs, outputs, and trade selection per lane | A villager, a valid workstation, trading materials, and energy |
| Farming Factory | Plants, grows, and harvests crops automatically | A villager, a supported seed or planting item, and energy |
| Iron Golem Factory | Runs iron golem production cycles and outputs drops | A villager and energy |

- **Independent trade controls:** Select trades globally or per lane, and pause or resume individual lanes.
- **Input distribution:** Distribute matching materials by complete transactions, consolidate idle remainders, and preserve inputs reserved by running trades.
- **Automation interfaces:** Configure item and energy sides, automatic output, Mekanism transporter settings, and redstone control.
- **Upgrades and management:** Upgrade windows, tier installer interactions, ownership, and security settings.
- **Visible interiors:** Glass windows show villagers and the relevant workstation, crop, or iron golem scene.
- **Localization:** Simplified Chinese and English are included.

## Requirements

| Mod | Required? | 1.20.1 Forge | 1.21.1 NeoForge |
| --- | --- | --- | --- |
| Mekanism | Yes | 10.4.16 or later | 10.7 or later |
| Mekanism Generators | Yes | 10.4.16 or later | 10.7 or later |
| Easy Villagers | Yes | 1.0.0 or later | 1.0 or later |
| Mekanism Extras | Optional | 1.5.0 up to, but not including, 1.6 | 1.4.1 or later |

These ranges come from the two branches' dependency declarations and do not mean every version has been tested. All dependencies must match your Minecraft version and loader and satisfy their own requirements.

Place Mekv and its required dependencies in the game's `mods` folder. Install them on both client and server for multiplayer, with matching Extras availability. AE2 and JEI are not declared as required dependencies of Mekv.

## Factory Tiers

| Tier | Trading lanes / output parallelism | Requires Extras |
| --- | ---: | :---: |
| Basic | 3 | No |
| Advanced | 5 | No |
| Elite | 7 | No |
| Ultimate | 9 | No |
| Absolute | 11 | Yes |
| Supreme | 13 | Yes |
| Cosmic | 15 | Yes |
| Infinite | 17 | Yes |

Without Extras, three factory types and four tiers provide **12 factories**. Installing Extras adds four higher tiers for **24 factories** and enables their recipes. Trading lane counts represent independent processes; farming and iron golem tiers increase output parallelism rather than shortening each cycle by the same factor.

Upgrade through crafting or use the matching tier installer while sneaking and right-clicking a placed factory. Check in-game recipes for materials. Back up your world and handle higher-tier machines before removing Extras: their blocks and items will no longer be registered.

## Getting Started

1. Craft and place a factory, then configure energy input and item input/output sides.
2. Insert an Easy Villagers villager item and, where applicable, a workstation or planting item.
3. For trading, select a valid offer and supply its materials. Farming and iron golem factories work automatically when their requirements are met.
4. Enable input distribution and automatic output as needed, set the redstone mode, and install supported upgrades.

### Trading Automation Notes

- Each lane has one input slot. **Trades requiring two different item types are not currently supported.**
- Global trade selection applies only to empty, idle lanes. Lanes with input or a running trade cannot immediately switch offers.
- Input distribution leaves paused lanes untouched. Resuming a lane allows redistribution again. A genuine remainder smaller than one transaction waits for more input.
- When creating processing patterns in AE2 or another system, use the villager's **current price**. Price changes or stock shortages can still prevent an order from completing.
- At a price of 4 iron for 1 emerald, 250 trades require 1000 iron. With distribution enabled, idle remainders are consolidated instead of requiring extra iron in every lane.
- If an order stalls, check paused lanes, current prices, stock, energy, redstone settings, and output space.

## Upgrades

| Factory | Supported upgrades |
| --- | --- |
| Trading Factory | Mekv's Infinite Trade Upgrade |
| Farming and Iron Golem Factories | Mekanism Speed, Energy, and Muffling upgrades |

The Infinite Trade Upgrade bypasses villager stock limits. It **does not remove material or energy costs**. Install and remove upgrades through the upgrade window, or install a supported upgrade by sneaking and right-clicking with it.

## Configuration

The configuration is generated at `config/mekv-common.toml` after startup. Exit the game or stop the server before editing, then restart to verify the changes.

| Option | Default | Meaning |
| --- | ---: | --- |
| `energyPerTrade` | 200 | Base energy cost per trade, FE |
| `energyPerGolem` | 1000 | Base iron golem processing cost, FE |
| `energyPerCropAge` | 50 | Base cost per crop growth step, FE |
| `energyPerHarvest` | 200 | Base crop harvest cost, FE |
| `tradeDurationTicks` | 15 | Trade duration per lane; accepts 10-20 ticks |
| `factoryEnergyCapacityMultiplier` | 1.0 | Factory energy capacity multiplier |
| `factoryEnergyTransferMultiplier` | 1.0 | Factory energy reception rate multiplier |

Supported upgrades can further affect energy costs and capacity. Some production and restocking timings follow Easy Villagers configuration; upgrade multiplier calculations use Mekanism configuration. At normal game speed, 20 ticks are approximately one second.

## Building From Source

Select the target branch first: `main` uses JDK 17, while `1.21.1neoforge` uses JDK 21. Each branch has its own local dependencies. Prepare the `libs/` JARs listed in that branch's `build.gradle`, including optional integrations and development helper mods.

```bat
gradlew.bat build
```

On Linux/macOS, use `./gradlew build`. Artifacts are written to `build/libs/`. Use `-PwithoutExtras` to exclude Extras from the development runtime; its JAR is still required for compilation.

Build checks cover trade distribution, UI layout, resource references, and JVM class loading with and without Extras. They do not replace in-game integration and visual testing.

## Reporting Issues

Include your Minecraft version, loader, Mekv and dependency versions, whether Extras is installed, and steps to reproduce. For trading issues, include the current price and paused-lane state. For UI issues, include a screenshot and GUI scale. For crashes, attach the log or crash report.

## Credits and License

Author: **Dasien**.

Thanks to Mekanism, Mekanism Extras, and Easy Villagers for their systems, APIs, and visual references. This project uses the [MIT License](LICENSE). Third-party code and assets remain subject to their respective licenses and attribution requirements.
