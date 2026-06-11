# Changelog

<!-- markdownlint-disable MD013 -->

## [ MariesLib 1.0.0 ] 2026-6-10

Initial release. MariesLib is the shared infrastructure library extracted from Nourished so
Marie mods can depend on one backbone instead of duplicating registries, scanner tooling,
tracking, compat, and datapack loaders.

### Added

- **Standalone shared library** for Marie mods — required separate install on the classpath,
  no JarJar bundling. Consuming mods wire runtime through `MarieLibContext` at bootstrap.
- **Scanner pipeline** — bulk classification of every edible/source item in a modpack:
  - Token stemming (`TokenStemmer`), archetype patterns, and recipe inheritance
  - Spread-based confidence validation (not hard thresholds)
  - Tag recommendation writer — ready-to-paste datapack JSON under `data/<modid>/tags/item/`
  - Multi-value analysis — secondary groups, overlap matrices, ambiguous-item reports
  - Scan cache and metrics (`ScannerMetrics`, `CacheStats`)
- **Classification traces** — inspectable per-item pipeline decisions (`ClassificationTrace`,
  `ClassificationTraceStep`, `ClassificationTraceFormatter`):
  - Tag lookup, resolver scores, blend precedence, confidence spread
  - Runtime stages: SIGNAL_AGGREGATION, WINNER_SELECTION, CONFIDENCE, TAG_RUNTIME_BLEND
- **Runtime classification** — gameplay-path resolution separate from bulk scanner:
  - `RuntimeResolver` with cascade stages and resolution outcomes
  - Tag/runtime blending (`ValueBlend`) with precedence rules (TAG vs RUNTIME_SUPPLEMENT)
  - Source families (`FamilyResolver`) and `config/<modid>/source_overrides.json`
- **Tracking system** — full player value progression infrastructure:
  - Memory windows (source, category, family) with time and count limits
  - Diminishing returns via logistic saturation curve
  - Debt tracking, streak bonuses, value decay, threshold effects
  - Source pair synergies, value synergies, milestones, tracking profiles
  - Season hooks and absorption modifiers
  - Persistent player data via `TrackingAttachment` codec
- **Three-tier compat system** (`ModCompat`, `CompatRegistry`):
  1. `data/<modid>/compat/compat_registry.json` in the consuming mod
  2. `data/<other_modid>/marie_compat.json` from loaded mods
  3. `config/<modid>/compat_overrides.json` for modpack overrides
  - Auto-compat discovery for unregistered food mods (`AutoCompatDiscovery`)
  - Conflict detection for effects, survival overhaul, and decay settings
- **Datapack loaders** (`MarieDataLoader`, schema v1):
  - Working now: `source_classifications/`, `compat/`, `source_families/`, `module_locks/`
  - Schema defined (loaders in progress): `values/`, `effects/`, `synergies/`,
    `source_synergies/`, `milestones/`, `tracking_profiles/`
  - Validation (`DatapackValidator`) and reload diagnostics (`DatapackDiagnostics`)
  - `/marie schema` command for sample JSON templates
- **Module system** — `ModuleCache` hot-path feature flags and server-side `LockRegistry`
  for datapack module locks
- **Config presets** — `PresetRegistry` with save/load/delete and compressed share-code
  import/export (`ImportExportManager`, Cloth Config widgets)
- **`/marie` commands** — `report`, `value`, `set`, `reset`, `profile`, `reload`,
  `invalidatecache`, `diagnostics`, `scan_analysis`, `schema`, `debug`
- **Client UX** — critical value toasts, preset cards, import/export screens, client
  tracking cache
- **Source application pipeline** — eating handlers, decay/effects handlers, reload
  pipeline, and tracking player events wired through `MarieLibContext`
- **`PresetRegistry` delegates** — `ensureBuiltInFilesOnDisk`, `applyPresetValues`, and
  `enableAllEffects` delegate through `MarieLibContext` so consuming mods own preset
  behavior without runtime crashes
- **Mod lifecycle** — `MariesLib` constructor accepts `ModContainer` for proper bootstrap
- **Mod icon** — `MariesLib_icon.png` for NeoForge/Modrinth metadata

### Architecture

- **`MarieLibContext` bootstrap** — consuming mod injects config suppliers, resolvers,
  screen factories, scanner callbacks, and preset delegates at startup
- **API lifecycle** — `MarieAPIState` registration phases; public types marked
  `@Stable`, `@Experimental`, or `@Internal` via `ApiStatus`
- **Registry framework** — `AbstractRegistry`, `RegistryLifecycleManager`,
  `RegistrySnapshot`; atomic reload with read/write locks during datapack reload
- **Handler pipeline** — source application → decay → effects → events, gated by
  `ModuleCache` flags and reload-in-progress state
- **Hot-path caching** — `ModuleCache` for tick/render loops; bounded LRU for scanner
  and resolver caches

### API

- **`MarieAPI`** (`@Stable`) — static entry point for consuming mods and addons:
  - Queries: `getValueLevel`, `getTotal`, `getSourceMemory`, `getTrackingData`,
    `modifyValue`, `getVersion`
  - Registration: values, source classifications, effects, compat, synergies, profiles,
    milestones, season hooks, absorption modifiers, report providers
- **`MarieEvents`** (`@Stable`) — `ValueChangedEvent`, `ValueCriticalEvent`,
  `ValueExcessEvent`, `SourceAppliedEvent`
- **`ValueModifierEvent`** — cancellable pre-apply modifier hook
- **`MarieLibContext`** (`@Stable`) — runtime context builder for bootstrap injection
- **`MarieLibRegistrationDelegate`** — value/effect/source registration contract
- **Experimental** — `ProfileDefinition`, `MilestoneDefinition`, `SynergyDefinition`,
  `SourcePairSynergy`, `AbsorptionModifier`, `MarieSeasonHook`, `ValueRenderer`,
  `ReportProvider`, and associated registries

### Integrations

- **KubeJS** — `MarieAPI` and `MarieEvents` script bindings via `kubejs.plugins.txt`
  and `META-INF/services` service loader (`MarieKubeJSPlugin`, `MarieKubeJSBindings`)
- **JEI / REI / EMI** — shared value tooltip helper (`MarieTooltipHelper`) for recipe
  viewers; all integrations are compileOnly optional dependencies
- **Cloth Config** — preset cards (`PresetsWidget`), import/export buttons
  (`ImportExportButtonsWidget`), save-preset screen (`SavePresetScreen`)

### Breaking Changes

- **`CompatDefinition` package move** (beta-period break, no deprecation shim):
  - **From:** `dev.maire.nourished.api.CompatDefinition`
  - **To:** `dev.marie.MariesLib.compat.CompatDefinition`
- **Tracking attachment schema reset** — legacy player tracking data from pre-extraction
  Nourished saves will not migrate; acceptable for the beta extraction period
- Future breaking API changes will include a deprecation shim and changelog notice

### Important Upgrade Notes

If upgrading from Nourished (or another Marie mod) before the MarieLib split:

1. Install **MariesLib 1.0.0+** alongside any Marie mod that depends on it (e.g.
   [Nourished 0.2.5-beta.5+](https://modrinth.com/mod/nourished)).
2. Update imports — shared types (`CompatDefinition`, scanner types, tracking types,
   registry helpers) now live under `dev.marie.MariesLib.*`.
3. Wire bootstrap through `MarieLibContext.Builder` and call `MarieLibContext.register()`
   after config is ready.
4. Register consuming-mod config **before** `MarieLibContext.register()` so module toggles
   sync into `ModuleCache` on load, reload, and config-screen apply.
5. For Nourished-specific config and scanner key renames, see the
   [Nourished 0.2.5-beta.5 changelog](https://github.com/kgbcupcake/nourished/blob/main/CHANGELOG.md).

### Notes

- Requires **Minecraft 1.21.1**, **NeoForge 21.1.x**, **Java 21**
- Datapack loaders for values, effects, synergies, milestones, and tracking profiles:
  schema and loader infrastructure are present; full datapack-only gameplay wiring is
  still in progress
- Network sync infrastructure is expanding — broader sync work remains on the roadmap
- First consuming mod: [Nourished](https://modrinth.com/mod/nourished) 0.2.5-beta.5+
- License: LGPL-3.0-only

## [ MariesLib 0.1.0-beta.1 ] 2026-6-10

First beta. Standalone MariesLib bootstrap, owned library config, source-trigger pipeline, and
preset/import-export refinements on top of the extracted backbone.

### Added

- **Standalone bootstrap** (`MariesLibBootstrap`) registers context, registries, and handlers when
  no consuming Marie mod registers `MarieLibContext` first; library config always lives in
  `config/marieslib.cfg`
- **Library config I/O** (`MariesLibConfigHolder`, `MariesLibConfigIO`, `MariesLibConfigKeys`,
  `MariesLibConfigBridge`) for load/save, preset snapshots, and import/export roots
- **Full Cloth Config screen** (`MariesLibClothConfig`) with categories for modules, thresholds,
  handlers, memory, scanner, client, and debug (`ClothCategory*` + `ClothConfigHelper`)
- **Dedicated import/export screens** (`MariesLibExportScreen`, `MariesLibImportScreen`) for file
  and share-code workflows
- **Source trigger pipeline** (`SourceApplicationPipeline`, `SourceTriggerRegistry`,
  `TriggerHandlerRegistry`, `ValueSourceTrigger`, `ISourceTriggerHandler`) for consuming-mod
  trigger registration separate from eating-only handlers
- **`SourceTriggerEvent`** in `MarieEvents`, fired when a registered source trigger runs
- **New registration APIs** (`@Stable`):
  - `registerSourcePropertySignal(SourcePropertySignal)` for scanner signal hooks owned by consuming mods
  - `registerSleepBonusEvaluator(SleepBonusEvaluator)` for wake-up bonus evaluators
  - `registerSourceTriggerHandler(ISourceTriggerHandler)` for NeoForge event subscription during setup
- **`amountScale` on `ValueDefinition`**, per-value scaling with validation in `MarieValidation`
- **`GuiValueRenderer`**, client `GuiGraphics` implementation of the `ValueRenderer` API marker
- **Debug memory logging toggle** (`debugMemoryLogging`) in the Debug Cloth category
  (`ClothCategoryDebug`)
- **Audit hardening**: lazy datapack loader init, `isRegistered()` guards on premature context
  access, idempotent handler/attachment registration, `ModuleCache.isInitialized()` checks, and
  null-safe client screen factory usage across widgets and commands

### Architecture

- **`ValueRenderer` split**: API marker in `api/`; client rendering contract in `client/GuiValueRenderer`
- **`MarieTooltipHelper`**: client-only `Minecraft` access gated on `Dist.CLIENT` and
  `ModuleCache.isInitialized()`
- **`MarieDataManager`**: lazy loader singleton avoids static init calling `DatapackSchema.root()`
  before context registration
- **`DatapackSchema.root()`**: falls back to `MariesLib.MOD_ID` when context is not yet registered
- **Cloth Config dependency**: `cloth-config-neoforge` is now an `implementation` dependency so
  the library ships its own config screen without a separate Cloth install
- **Source item detection**: `MarieCommand`, `ItemScanner`, `ClassificationTraceFormatter`,
  EMI/REI/JEI plugins, and tooltips use `MarieLibContext.sourceItemFilter()` instead of hard-coded
  `FoodProperties` checks
- **Preset storage**: presets live under `config/marieslib/presets/` via `MariesLib.MOD_ID`;
  locked presets are seeded by the active Marie mod through
  `MarieLibContext.ensureBuiltInPresetsOnDisk()`, not MariesLib-owned Casual/Survival/Hardcore JSON
- **`ParsedPreset` model**: removed `builtin` flag; delete/lock UI uses `locked` only; list sorts
  locked presets first, then alphabetically by name
- **Preset application**: `PresetRegistry.applyPresetValues()` writes through
  `MariesLibConfigHolder`, persists with `MariesLibConfigIO.save()`, and refreshes `ModuleCache`;
  removed hardcore-only `enableAllEffects` side effect
- **Import/export paths**: `ImportExportManager` share prefix, export directory, and filenames use
  `MariesLib.MOD_ID` instead of the consuming mod id
- **Config screen transitions**: `PresetsWidget` and `SavePresetScreen` reopen via
  `MariesLibClothConfig.create()`; save-current reads `MariesLibConfigHolder.toPresetValues()`
- **Config load timing**: `MariesLibConfigIO.load()` runs in the `MariesLib` constructor before
  bootstrap
- **`MarieDataLoader.Callbacks`**: registration methods use typed API parameters instead of raw
  `Object`
- **Hardcoded paths and keys**: datapack relative paths use `DatapackSchema` constants; config keys
  use `MariesLibConfigKeys`; JEI plugin and config filename use `MariesLib.MOD_ID`
- **Preset JSON keys**: `enableDecay` / `enableEffects` exposed as `PresetRegistry` constants

### API

- **`MarieAPI` / `MarieAPIState`**: `assertRegistrationAllowed(method)` replaces generic closed
  registration errors; `registerValue` also registers into `ValueRegistry`; classification amount
  validation accepts any finite float
- **`SourcePropertySignal`**, **`SleepBonusEvaluator`**, **`SourceTriggerDefinition`**, and
  **`ValueSourceTrigger`**, consuming-mod extension points for scanner signals, sleep bonuses, and
  gameplay triggers
- **`MarieKubeJSStartupEvents`**: lazy `registerValues()` / `registerProfiles()` /
  `registerMilestones()` methods replace static fields that called `get()` at class init
- **`ClothCategoryMemory`** is public so consuming mods can embed memory/diminishing-returns entries
  in their own config screens

### Integrations

- **Cloth Config**: full library-owned config screen in addition to preset cards (`PresetsWidget`),
  import/export buttons (`ImportExportButtonsWidget`), and save-preset screen (`SavePresetScreen`)
- **KubeJS**: startup event id resolution deferred until context is registered

### Breaking Changes

- **`ParsedPreset` record**: `builtin` component removed; use `locked()` for lock/delete rules
- **`MarieLibContext` preset delegates**: `enableAllEffectsForPresets` removed; preset side effects
  are owned by the consuming mod's `ensureBuiltInPresetsOnDisk()` implementation
- **Built-in preset files**: MariesLib no longer writes Casual/Survival/Hardcore JSON on disk;
  consuming mods must seed their own locked presets

### Important Upgrade Notes

If upgrading to MariesLib 0.1.0-beta.1 from the 1.0.0 extraction baseline:

1. Preset snapshots now load from `config/marieslib/presets/`; only locked presets are
   non-deletable.
2. Consuming Marie mods must implement `ensureBuiltInPresetsOnDisk()` to seed their own locked
   presets; MariesLib no longer writes Casual/Survival/Hardcore files on first run.
3. Import/export share codes and export files use the `marieslib` mod id prefix and
   `config/marieslib/exports/` path.

### Notes

- Published artifact version is **0.1.0-beta.1** (`gradle.properties`)
- Preset list description updated in `en_us.json` for the new presets-folder behavior
