# MariesLib API

MariesLib is a NeoForge 1.21.1 library for tracking player values, classifying source items, and wiring decay, memory, effects, and progression. Consuming mods register their own value keys and hook up gameplay through `MarieLibContext`. Nourished is the reference consumer — if you only care about nutrition, see [Nourished API](https://github.com/kgbcupcake/nourished/blob/main/API.md) instead.

## Getting started

Add MariesLib as a compile-time dependency:

```gradle
repositories {
    maven { url = "https://maven.neoforged.net/releases" }
}

dependencies {
    compileOnly "dev.marie.MariesLib:marieslib:<version>"
}
```

At runtime, MariesLib must be installed as a separate mod. There is no JarJar bundling.

Minimum setup in your `@Mod` constructor:

```java
@Mod("examplemod")
public final class ExampleMod {
    public ExampleMod(IEventBus modBus) {
        if (!ModList.get().isLoaded("marieslib")) return;

        MariesLibBootstrap.attach("examplemod", modBus);

        MarieAPI.registerValue(ValueDefinition.builder("emc")
                .displayName("EMC")
                .color(0xFF44AAFF)
                .defaultDecayRate(0.002f)
                .build());
    }
}
```

Guard every API call with `ModList.get().isLoaded("marieslib")` so your mod still loads when the library is absent.

## Building on MarieLib vs using it through Nourished

| You want to… | Depend on |
|--------------|-----------|
| Add nutrients, food mappings, diet events | **Nourished** (`NourishedAPI`) |
| Ship a new Marie mod with its own value bars | **MariesLib** (`MarieAPI` + `MarieLibContext`) |

This document covers MariesLib directly.

## Registration window

All `MarieAPI.register*` calls must happen during mod initialization — inside your `@Mod` constructor or an `FMLCommonSetupEvent` handler.

The window closes after init completes. Calling register outside that window throws `IllegalStateException`.

Datapack reloads open a secondary window internally for datapack-driven content. Addon mods do not manage that themselves.

- Register in `@Mod` constructor or common setup
- Do not register lazily on first use
- Do not register from gameplay event handlers

## Core concepts

**Value** — A tracked bar (0.0–1.0) with decay and thresholds. Registered via `ValueDefinition`. Example keys: `emc`, `stamina`, or in Nourished: `proteins`, `grains`.

**Source** — Anything that applies value: an item (`ResourceLocation`), a custom trigger (`ValueSourceTrigger`), or a classification mapping.

**Category** — The dominant value key assigned when a source is applied. Used for category-level memory and fatigue. Not a separate registry entry.

**Family** — Optional string grouping (`"fish"`, `"bread"`) for family-level fatigue. Resolved by the consuming mod's `sourceFamilyResolver` hook.

**Classification** — The pipeline that inspects a source item and produces value scores. Tags, scanner signals, and runtime resolvers all feed into it.

**Memory** — Server-side maps on `TrackingData`: `sourceMemory`, `categoryMemory`, `familyMemory`. Drives diminishing returns. Never sent raw to the client.

> A value is a bar. A source is what fills it. Memory is what you applied recently.

## MarieAPI reference

All methods are in `dev.marie.MariesLib.api.MarieAPI`.

### Player queries

```java
float getAggregateLevel(Player player)
float getValueLevel(Player player, String valueKey)      // -1 if unknown key
ApplicationHistoryView getApplicationHistory(Player player)
float getTotalCount(Player player)                       // alias for getAggregateLevel
MariePlayerData getTrackingData(Player player)
void modifyValue(Player player, String valueKey, float delta)
String getVersion()
```

`modifyValue` posts a `ValueModifierEvent` first. Listeners can cancel or change the amount before it lands.

### Registration — values and sources

```java
void registerValue(ValueDefinition definition)           // alias: addValue
void registerSourceClassification(ResourceLocation sourceId, String valueKey, float amount)
void registerSource(ResourceLocation sourceId, String valueKey, float amount)  // alias
```

### Registration — effects, compat, synergies

```java
void registerCustomEffect(ThresholdEffect definition)    // alias: addEffect
void registerCompatEntry(CompatDefinition definition)    // alias: addCompat
void registerValueSynergy(SynergyDefinition definition)  // alias: addValueSynergy
void registerSourcePairSynergy(SourcePairSynergy def)    // alias: addSourceSynergy
void registerTrackingProfile(ProfileDefinition definition) // alias: addProfile
void registerMilestone(MilestoneDefinition definition)   // alias: addMilestone
```

`CompatDefinition` lives in `dev.marie.MariesLib.compat`.

### Registration — hooks

```java
void registerSeasonHook(MarieSeasonHook hook)            // alias: addSeasonHook
void registerAbsorptionModifier(AbsorptionModifier mod)  // alias: addAbsorptionModifier
void registerReportProvider(ReportProvider provider)     // alias: addReportSection
void registerSourcePropertySignal(SourcePropertySignal signal)
void registerSleepBonusEvaluator(SleepBonusEvaluator evaluator)
void registerTriggerHandler(SourceTriggerListener handler)
void registerTriggerSource(SourceTriggerDefinition definition)  // alias: addTriggerSource
```

### Custom triggers

For non-item sources (crafting, EMC, block breaks):

```java
void fireSourceTrigger(ServerPlayer player, ValueSourceTrigger trigger)
void fireSourceTrigger(ServerPlayer player, ValueSourceTrigger trigger, @Nullable ItemStack stack)
```

Server-side only. Runs the full pipeline: classification, modifiers, synergies, threshold checks.

## ValueDefinition

```java
ValueDefinition.builder("my_value")
    .displayName("My Value")
    .color(0xFF66CCFF)
    .defaultDecayRate(0.001f)
    .criticalThreshold(0.1f)
    .lowThreshold(0.3f)
    .excessThreshold(0.9f)
    .beneficial(true)
    .amountScale(1.0)
    .build()
```

| Field | Meaning |
|-------|---------|
| `id` | Internal key, used everywhere |
| `displayName` | HUD / UI label |
| `color` | ARGB bar color |
| `defaultDecayRate` | Per-tick decay when above zero |
| `criticalThreshold` | Fires `ValueCriticalEvent` when crossed below |
| `lowThreshold` | HUD warning band |
| `excessThreshold` | Fires `ValueExcessEvent` when crossed above |
| `beneficial` | Whether high is good (affects some UI hints) |
| `amountScale` | Scales raw application amounts before normalization |

Other definition types (`ThresholdEffect`, `SynergyDefinition`, `SourcePairSynergy`, `ProfileDefinition`, `MilestoneDefinition`) follow the same builder pattern. Check the Javadoc on each class for required fields.

## NeoForge events

Subscribe on the NeoForge event bus (`@SubscribeEvent`):

| Event | When | Cancellable |
|-------|------|-------------|
| `MarieEvents.ValueChangedEvent` | After a value level changes | No |
| `MarieEvents.ValueCriticalEvent` | Value drops below critical threshold | No |
| `MarieEvents.ValueExcessEvent` | Value rises above excess threshold | No |
| `MarieEvents.SourceTriggerEvent` | Before pipeline processes a trigger | Yes |
| `MarieEvents.SourceAppliedEvent` | After a source applies value | No |
| `ValueModifierEvent` | Before `modifyValue` applies a delta | Yes |

Use `ValueModifierEvent` or `SourceTriggerEvent` to intercept. Use the `*Changed` / `*Critical` / `*Excess` events to react after the fact.

## KubeJS

KubeJS support is `@Experimental`. May change between releases.

**Startup bindings** (`MarieAPI`):

```js
MarieAPI.registerValue({
    id: 'custom_value',
    displayName: 'Custom Value',
    color: 0x66CCFF,
    decayRate: 0.002,
    critical: 0.1,
    low: 0.3,
    excess: 0.9
})

MarieAPI.registerSourceClassification('minecraft:apple', 'custom_value', 0.35)
```

**Server bindings** (`MarieServer`):

```js
let level = MarieServer.getValueLevel(player, 'custom_value')
MarieServer.forceSetValue(player, 'custom_value', 0.5)
```

**Events** (`MarieEvents`):

```js
MarieEvents.valueChanged(event => {
    if (event.valueKey === 'custom_value' && event.newValue < 0.25) {
        // react
    }
})
```

Event IDs: `valueChanged`, `valueCritical`, `valueExcess`, `sourceConsumed`, `valueDeltaModifier`, `decayTick`, `playerSynced`.

## Datapack paths

Under `data/<namespace>/<modid>/`:

| Path | Purpose |
|------|---------|
| `source_classifications/` | Item → value key mappings |
| `compat/` | Mod compat declarations |
| `source_families/` | Family groupings for fatigue |
| `module_locks/` | Server-side feature locks |

Schemas for `values/`, `effects/`, `synergies/`, `milestones/`, `tracking_profiles/` exist but loaders are still being completed. Check CHANGELOG for current status.

Config overrides live under `config/<modid>/` — see ARCHITECTURE.md for the priority stack.

## Commands

MariesLib registers two command roots:

- `/marieslib` — library diagnostics, scanner tooling, schema templates
- `/marie` — generic consumer command tree (report, value, set, reset, profile, reload)

Consuming mods register their own namespace via `MarieConsumerCommandTree` — Nourished uses `/nourished`, for example.

## Integration bootstrap

`MariesLibBootstrap.attach(modId, modEventBus)` is the minimal entry. For custom classification stages, family resolvers, sync, or config screens, use `MarieLibContext.builder(modId)` and fill the hooks before `build()`.

See `MariesLibBootstrap` Javadoc and Nourished's `NourishedContextBuilder` for a full consumer example.

## Versioning

```java
MarieAPIVersion.VERSION          // "1.0.0"
MarieAPIVersion.isCompatible(1)  // true if MAJOR >= required
```

### Stability tiers

**`@ApiStatus.Stable`** — Safe for released addons. No breaking signature changes within a minor version.

**`@ApiStatus.Experimental`** — May change any release. KubeJS bindings are experimental.

**`@ApiStatus.Internal`** — Not a public contract. Do not import.

Stable surface today: `MarieAPI`, `ValueDefinition`, `ThresholdEffect`, `SynergyDefinition`, `SourcePairSynergy`, `ProfileDefinition`, `MilestoneDefinition`, `CompatDefinition`, `MarieEvents`, `ValueModifierEvent`, `ApplicationHistoryView`, `MariePlayerData`, `MarieAPIVersion`, `MariesLibBootstrap.attach`, `MarieLibContext` stable methods.

For the full stability contract, see [PHILOSOPHY.md](PHILOSOPHY.md).
