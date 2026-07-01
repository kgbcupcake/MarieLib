# Changelog

<!-- markdownlint-disable MD013 -->

## [ MariesLib 0.1.1-beta.3 ] 2026-6-30

Source-pair synergy value buffs, source classification unification, tag-audit and export
command wiring, and player-logout state cleanup for the new synergy tracking.

### Added

- **`MarieAPI.registerConfigValidator(validator)`**: now validates that the `validator`'s
  `modId()` is non-empty and matches a registered mod; throws `IllegalArgumentException` if not
- **`SourcePairSynergy.getValueModifier()` / `getModifierDurationTicks()`** (`@ApiStatus.Stable`),
  with matching `Builder.valueModifier(float)` / `Builder.modifierDurationTicks(int)`: an optional
  temporary value-gain multiplier applied when the pair synergy fires, lasting the given number
  of game ticks
- **`SynergyBuffTracker`** (`tracking`): per-player, per-value-key temporary modifier store keyed
  off `player.level().getGameTime()`; `activate(playerId, valueKey, modifier, expiryTick)` sets a
  buff, `getActiveModifier(playerId, valueKey, currentTick)` reads it back and evicts it in place
  once expired; `clearPlayer(playerId)` drops all buffs for a player
- **`SynergyAbsorptionModifier`** (`tracking`, implements `AbsorptionModifier`): stateless bridge
  that applies `SynergyBuffTracker`'s active modifier during value absorption; registered in
  `MariesLibBootstrap` via `MarieAPI.registerAbsorptionModifier`
- **`SourceApplicationPipeline.clearPlayer(playerId)`**: clears the per-player synergy last-fired
  and value-synergy active-state maps; wired into the existing `PlayerLoggedOutEvent` handler
  (`MarieCommandSupport.onPlayerLoggedOut`) alongside the new `SynergyBuffTracker.clearPlayer`
  call, so synergy state no longer persists in memory after a player disconnects
- **Datapack schema keys** `value_modifier` / `modifier_duration_ticks`
  (`DatapackSchema.KEY_VALUE_MODIFIER` / `KEY_MODIFIER_DURATION_TICKS`) for
  `data/<namespace>/<modid>/source_synergies/<id>.json`, both optional (default `1.0` / `0`)
- **`MarieDataLoader.parseSynergy` / `parseSourcePairSynergy`**: datapack loading for
  `synergies/` and `source_synergies/` is now implemented (previously threw
  `UnsupportedOperationException` and skipped the file)
- **`CommandCapability`** / **`CommandCapabilityRegistry`** (`@ApiStatus.Experimental`):
  consuming-mod-registered command handlers keyed by `(modId, capability)` resource locations,
  via `MarieAPI.registerCommandCapability`
- **`ExportWriter.writeExport(resolverId)`** and **`RegistryExporter`**: runs a registered
  `ExportResolver` over its target registry and writes
  `config/<modid>/<resolverId>_export.json`; wired into `/marieslib dump <resolverId>` /
  `/marie dump <resolverId>`
- **`TagAuditReportWriter`**: writes a full `TagReport` (from `TagScanner.scan`) to disk; wired
  into a new `/marieslib audit_tags <modid>` command via `MariesLibCommand.runAuditTags`
- **`MariesLibCommand.runDump` / `runAuditTags`**: command handlers backing the `dump` and
  `audit_tags` subcommands
- **`SourceClassificationRegistry`** rewritten as a full config-backed registry (was previously a
  thin read-only view over `SourceRegistry`): `load()` / `reload()` / `loadFromDatapack()` read
  `config/<modid>/source_classifications.json`, auto-migrating existing
  `source_overrides.json` + `source_values.json` on first run; adds `get(sourceId)`,
  `getOverride(sourceId)`, `getScore(itemId, valueKey)`, and `setOverride(itemId, values, enabled)`
- **`RuntimeResolver` classification trace**: when a live external/scanner override exists for an
  item, a new `EXTERNAL_CLASSIFICATION` trace step is recorded explaining that gameplay uses the
  cached override rather than the live inference result shown below it

### Changed

- **`SourceOverrideRegistry` and `SourceValueRegistry` removed**, merged into the rewritten
  `SourceClassificationRegistry`; `MarieKubeBindings.registerClassification` and
  `MarieLibContext` scanner-score lookups now call `SourceClassificationRegistry` directly
- **`MarieAPI`**: registration methods reordered/regrouped (export resolvers, config validators,
  tag audit, and the new command-capability registration now live together near the end of the
  class); no functional change to existing methods
- **`MariesLib` constructor**: no longer auto-bootstraps `MariesLibBootstrap` when
  `MarieLibContext` is unregistered — this call was already removed from `MarieAPI`'s
  fireSourceTrigger path in 0.1.1-beta.1's changelog notes but the constructor still had it;
  consuming mods must call `MariesLibBootstrap.attach` / `bootstrap` explicitly
- **`RecipeInheritanceResolver.buildIndex(RecipeManager)`**: now actually builds the ingredient
  index (`this.recipeIndex = buildIndex()`) instead of only storing the `RecipeManager` reference
- **`PresetRegistry.PresetValues`**: replaced the fixed-field record (`decayRate`,
  `criticalThreshold`, `lowThreshold`, `excessThreshold`, `defaultEffectDurationTicks`,
  `enableDecay`, `enableEffects`) with a single opaque `JsonObject values` passthrough, so
  consuming mods can store arbitrary preset schemas instead of MariesLib-defined fields
- **`MarieValueColors.resolvedDefaultArgb(key)`**: no longer an alias for `baseColorArgb`; now
  resolves `ValueDefinition.getColorOverride()` first, then falls back to the palette-only color,
  bypassing transient UI overrides and `ColorRegistry` as intended for "is this actually
  customized" checks
- **`ModCompat` classpath resource loading** (`loadTier1BuiltIn`, tier-2 mod loop): reads
  `compat_registry.json` / `marie_compat.json` via the current thread's context class loader
  first, falling back to `ModCompat.class.getResourceAsStream`, matching the same fix already
  applied to `ScannerSpecRegistry` in 0.1.1-beta.1
- **`ScannerSpecRegistry.parseBundled()`**: also routes through the context class loader

### Fixed

- **`ColorRegistry.parseArgbString` `0x`/`0X` branch**: parses the hex portion via
  `Long.parseLong(hex, 16)` with `0xFF_FF_FF_FF` masking instead of `Integer.decode`, avoiding
  the same signed-decode inconsistency already fixed for other branches in 0.1.0-beta.5
- **`MarieDebugCommand` trace dump filename**: `analyze <item>` writes to
  `trace_dump_<item>_<timestamp>.txt` instead of a single shared `trace_dump.txt`, so repeated or
  concurrent analyses no longer overwrite each other's output

### Notes

- Published artifact version is **0.1.1-beta.3** (`gradle.properties`)

---

## [ MariesLib 0.1.1-beta.2 ] 2026-6-27

Config validation framework, tag-audit system, curve grid math, command extensions, and scanner spec graceful-skip.

### Added

- **`ConfigValidator`** (`@ApiStatus.Stable`): consuming-mod hook that validates its own configuration and
  returns a structured `ValidationResult`; registered via `MarieAPI.registerConfigValidator`
- **`ConfigValidatorRegistry`** (`@ApiStatus.Internal`): keyed store of `ConfigValidator` instances;
  supports `getAllRaw()` for internal runner iteration
- **`Finding`** (`@ApiStatus.Stable`): record carrying `severity`, `file`, `key`, and `message` for a
  single validation issue within a `ValidationResult`
- **`ValidationResult`** (`@ApiStatus.Stable`): structured outcome of a config validator run; status is
  one of `PASS`, `WARN`, or `FAIL`, plus a list of `Finding` items
- **`ValidationRunner.runForMod(modId)`** (`@ApiStatus.Stable`): filters and runs only the validators
  belonging to the given mod id; complements `runAll()` for per-consumer diagnostics
- **`TagAuditContext`** (`@ApiStatus.Stable`): context interface passed to tag-audit rules; exposes
  `knownCategories()`, `itemsInCategory(category)`, `categoriesForItem(itemId)`,
  `liveInferenceLookup()`, and `namespacesPresent()`
- **`TagRule`** (`@ApiStatus.Stable`): interface for pluggable tag-audit rules; implements
  `findIssues(context)` and `suggestFixes(context, issues)`
- **`TagIssue`** (`@ApiStatus.Stable`): record describing a single tag problem with `severity`, `category`,
  `itemId`, and `message`
- **`TagFixSuggestion`** (`@ApiStatus.Stable`): record carrying a corrective suggestion (`category`,
  `itemId`, `action`, `reason`) produced by a `TagRule`
- **`TagAuditSeverity`** (`@ApiStatus.Stable`): severity enum (`ERROR`, `WARN`, `INFO`) for `TagIssue`
- **`TagReport`** (`@ApiStatus.Stable`): result of a full `TagScanner.scan` run; includes `timestamp`,
  `rulesRun`, `issues`, and `suggestions`
- **`TagScanner.scan(context)`** (`@ApiStatus.Stable`): iterates all registered `TagRule` instances,
  collects issues and fix suggestions, and returns a timestamped `TagReport`
- **`TagRuleRegistry`** (`@ApiStatus.Internal`): ordered store of registered `TagRule` instances
- **`TagAuditContextRegistry`** (`@ApiStatus.Internal`): per-modId store of `TagAuditContext` instances
- **`CurveGrid`** (`@ApiStatus.Stable`): 2-D grid of float multipliers evaluated via bilinear
  interpolation; axes are `(intensity, confidence)` both normalised to `[0, 1]`; `flat(xCells, yCells,
value)` constructs a uniform grid, `evaluate(x, y)` samples with clamping
- **`CurveGridJson`** (`@ApiStatus.Internal`): JSON serialisation / deserialisation for `CurveGrid`
- **`SourceClassificationRegistry`** (`@ApiStatus.Stable`): public read-only view of external source
  classifications registered via `SourceRegistry`; `getAll()` returns an unmodifiable map of
  `ResourceLocation → SourceClassification(sourceId, values)`
- **`MarieAPI.registerConfigValidator(validator)`** (`@ApiStatus.Stable`): public registration entry
  point for `ConfigValidator` instances
- **`MarieAPI.registerExportResolver(resolver)`** and **`registerExportResolver(key, registryKey, resolver)`**
  (`@ApiStatus.Stable`): two overloads for wiring `ExportResolver` instances into `ExportResolverRegistry`
- **`MarieAPI.registerTagRule(rule)`** and **`MarieAPI.registerTagAuditContext(modId, context)`**
  (`@ApiStatus.Stable`): public registration entry points for the tag-audit system
- **`/<modid> set_all <value> <player>`** (`MarieConsumerCommandTree`, permission 2): sets all
  registered value keys for the target player to the given normalised level in a single command via
  `SourceApplicationPipeline.writeDirectValue`
- **`/<modid> validate`** (`MarieConsumerCommandTree`): runs all `ConfigValidator` instances
  registered for the consumer mod and prints PASS / WARN / FAIL results with per-finding detail in
  colour-coded chat; backed by `MarieValidationCommands`
- **`/<modid> analyze <item>`** (`MarieConsumerCommandTree`, permission 0): resolves a
  `ClassificationTrace` for any food item via `RuntimeResolver` and writes a full inspector dump to
  `config/<modid>/debug/analyze_<item>_<timestamp>.txt`; suggests only food-bearing items via
  `DataComponents.FOOD` filter
- **`MarieValidationCommands`** (`@ApiStatus.Internal`): internal command handler for the `validate`
  subcommand; formats `ValidationResult` output with `ChatFormatting` colour coding
- **`SourceRegistry.getAllExternalClassifications()`**: returns a snapshot of all API- and KubeJS-
  registered classifications for external inspection

### Changed

- **`MariesLibCommand`**: removed duplicate `registerLibraryTree` call under `MariesLib.MOD_ID`; the
  library command tree is now registered only under the `/marie` alias
- **`RecipeInheritanceResolver.getIngredients()`**: promoted to `public` visibility; added `buildIndex(RecipeManager)`
  to set the active recipe manager for ingredient index construction
- **`MarieValueColors`**: additional accessor method(s) to support tag-audit and export tooling

### Fixed

- **`ScannerSpecRegistry.writeBundledTo()`**: now returns `boolean` instead of `void`; the caller
  (`ensureLoaded`) only logs a success message when `true` is returned and silently skips at
  `DEBUG` level when no bundled `scanner_spec.json` exists for the active modId, eliminating
  spurious warnings for mods that do not ship a bundled spec

### Notes

- Published artifact version is **0.1.1-beta.2** (`gradle.properties`)

---

## [ MariesLib 0.1.1-beta.1 ] 2026-6-20

Generic registry-export framework, scanner spec loading fix, explicit bootstrap requirement, and registry frozen-state exposure.

### Added

- **`ExportResolver<T>`** (`@ApiStatus.Stable`): consuming-mod hook that resolves per-entry export
  data for an entire registry; registered via `MarieAPI.registerExportResolver`
- **`ExportResolverRegistry`** (`@ApiStatus.Internal`): keyed store of export resolvers and their
  target `ResourceKey<? extends Registry<T>>`; never frozen so resolvers can register throughout mod init
- **`RegistryExporter.run(resolverId)`** (`@ApiStatus.Internal`): iterates the resolver's target
  registry via `BuiltInRegistries.REGISTRY`, calls `ExportResolver.resolve` per entry, and returns
  non-empty results keyed by entry id
- **`ExportWriter.writeExport(resolverId)`** (`@ApiStatus.Internal`): runs `RegistryExporter` and
  writes `config/<modid>/<resolverId>_export.json` as a sorted JSON array of
  `{"id": "<resource location>", "data": {...}}` objects; nested `Map` values serialize as JSON objects
- **`/marieslib dump <resolverId>`** and **`/marie dump <resolverId>`** (`MariesLibCommand`): runs
  `ExportWriter.writeExport` and reports the output path or a failure message
- **`MarieAPI.registerExportResolver(resolverId, registryKey, resolver)`** (`@ApiStatus.Stable`):
  public registration entry point with `MarieAPIState` phase guard and blank-`resolverId` validation
- **`ValueRegistry.isFrozen()`** (`@ApiStatus.Internal`): exposes the underlying
  `AbstractRegistry` frozen state via `INSTANCE.isFrozen()`, matching the existing
  `freezeInternal()` / `resetInternal()` internal lifecycle surface

### Changed

- **`MariesLib` constructor**: no longer auto-calls `MariesLibBootstrap.bootstrap()` when
  `MarieLibContext` is unregistered; consuming mods must bootstrap explicitly via
  `MariesLibBootstrap.attach` / `bootstrap`

### Fixed

- **`ScannerSpecRegistry` bundled spec loading** (`parseBundled`, `writeBundledTo`): reads
  `scanner_spec.json` via `Thread.currentThread().getContextClassLoader()` (with the leading
  `/` stripped from the resource path) instead of `ScannerSpecRegistry.class.getResourceAsStream`,
  so specs packaged in the consuming mod's jar (e.g. Nourished) resolve correctly instead of
  failing with `Bundled scanner_spec.json missing`

### Notes

- Published artifact version is **0.1.1-beta.1** (`gradle.properties`)

---

## [ MariesLib 0.1.0-beta.5 ] 2026-6-16

Color resolution and ARGB parsing fixes for value customization detection.

### Added

- **`MarieValueColors.resolvedDefaultArgb(String key)`**: returns a value key's true default ARGB,
  checking `ValueDefinition.colorOverride` then the built-in palette, while bypassing transient
  `OVERRIDES` and `ColorRegistry`; use this (not `paletteOnlyArgb`) when deciding whether an
  edited color is an actual customization
- **`IMarieLibConfig.decayRateFor(String valueKey)`** and **`MarieLibContext.Builder.decayRateFor(Function<String, Float>)`**:
  per-value decay rate resolver wired through config context; defaults to
  `ValueDefinition.getDefaultDecayRate()` with `0.001f` fallback when the key is unknown

### Fixed

- **`ColorRegistry.parseArgbString` `0x`/`0X` branch**: parses hex via `Long.parseLong` with
  `0xFF_FF_FF_FF` masking instead of `Integer.decode`, matching the `#RRGGBB` and bare 8-digit
  branches and avoiding signed-decode inconsistencies on full ARGB literals
- **`ValueDecayListener` decay tick rate**: uses `IMarieLibConfig.get().decayRateFor(key)` instead
  of always reading `ValueDefinition.getDefaultDecayRate()`, so consuming-mod overrides take effect
- **`MarieCommandSource` / `MariePlayerCommands` decay display**: report and value-detail commands
  route decay rates through `IMarieLibConfig.decayRateFor(key)` instead of hard-coded definition
  defaults
- **`SourceRegistry.clearExternalClassifications()`**: API/KubeJS `registerClassification()`
  entries are tracked in `API_REGISTERED_CLASSIFICATIONS` and re-applied after each clear, so
  classifications registered at mod construction survive `TagsUpdatedEvent` reload passes
  (datapack-sourced entries are still cleared and reloaded normally)
- **`CriticalValueToast` value name**: falls back to the raw `valueKey` when the tracking-bar
  translation key is missing, instead of showing the untranslated key path in the toast
  title/subtitle

### Notes

- Published artifact version is **0.1.0-beta.5** (`gradle.properties`)

---

## [ MariesLib 0.1.0-beta.4 ] 2026-6-16

Milestone tooling, per-value color overrides, datapack effect loading, and build stability.

### Added

- **`ValueDefinition.colorOverride`**: optional per-value ARGB stored on the definition;
  `MarieValueColors.baseColorArgb` prefers transient UI override, then `ColorRegistry`, then
  `colorOverride`, then the built-in palette
- **`MilestoneRegistry.getForAll()`**: returns milestones whose `valueKey` is `"all"` (every
  registered value key must reach the cumulative goal)
- **`MilestoneTracker` cross-nutrient milestones**: after per-key milestone checks, evaluates
  `"all"` milestones against every key in `MarieLibContext.valueKeys()`, grants rewards, and
  posts `MilestoneTriggeredEvent` with `valueKey` `"all"`
- **`generate_milestone_template` command** (`MarieMilestoneTemplateCommand`): writes a starter
  milestone + advancement datapack to `<world>/datapacks/<modid>-milestone-template/`
- **`MarieDataLoader.parseEffect`**: loads threshold-effect datapack entries (previously threw
  `UnsupportedOperationException` and skipped files)
- **`SourceRegistry.clearSessionWarnings()`**: resets per-session dedupe for external
  classification cap warnings

### Changed

- **`DatapackValidator`**: ignores JSON fields prefixed with `_comment_` instead of warning on
  them as unknown schema fields
- **`SourceRegistry.registerClassification`**: cap-reached warnings log once per `sourceId` per
  session; successful registrations log at debug instead of info
- **Compile dependency pins** (`gradle.properties`, `build.gradle`): `jei_version`, `rei_version`,
  and `emi_version` replace `+` ranges; `cloth-config-neoforge` forced to
  `${cloth_config_version}` so Java 21 builds do not resolve future Java 25 artifacts
- **GitHub Actions publish workflow**: `actions/checkout@v4` → `actions/checkout@v5`

### Notes

- Published artifact version is **0.1.0-beta.4** (`gradle.properties`)

---

## [ MariesLib 0.1.0-beta.3 ] 2026-6-15

Milestone tracking release. Cumulative intake milestones are now tracked at runtime,
fired as events, loadable from datapacks, and exposed to KubeJS.

### Added

- **`MilestoneTriggeredEvent`** in `MarieEvents`, fired after a player completes a cumulative
  intake milestone and configured rewards are applied
- **`MilestoneProgressData`**, per-player cumulative intake totals and one-shot completion
  bookkeeping, serialized separately from `TrackingData`
- **`MilestoneProgressAttachment`**, NeoForge data attachment (`milestone_progress`) with
  `copyOnDeath()`, gated by `FeatureFlagCache.enableMilestones()`
- **`MilestoneTracker`**, accumulates positive value intake, detects first-time completions,
  grants mob-effect and advancement rewards, then posts `MilestoneTriggeredEvent`
- **`MarieDatapackCallbacks`**, default `MarieDataLoader.Callbacks` implementation that delegates
  datapack registrations to `MarieAPI` (values, effects, synergies, milestones, profiles, compat,
  source families, module locks)
- **Datapack milestone parsing** in `MarieDataLoader` for
  `data/<namespace>/<modid>/milestones/<id>.json` (`valueKey`, `cumulativeGoal`, optional
  `rewardEffectId`, `amplifier`, `rewardDuration`, `advancementId`)
- **Bootstrap wiring** in `MariesLibBootstrap`: registers `MilestoneProgressAttachment` and sets
  `MarieDataManager.setCallbacks(MarieDatapackCallbacks.INSTANCE)` on standalone bootstrap
- **`advancementId` on `MarieKubeBindings.registerMilestone`**, optional KubeJS spec key mapped to
  `MilestoneDefinition.Builder.advancement()`

### Architecture

- **Pipeline hook**: `SourceApplicationPipeline` calls `MilestoneTracker.onValueApplied(player,
key, finalDelta)` after each positive per-valueKey delta is applied and `SourceAppliedEvent` is
  posted
- **Reward flow**: `MilestoneTracker` applies optional mob effects via `BuiltInRegistries.MOB_EFFECT`,
  awards advancements through the server advancement manager, and logs warnings for unknown
  effect/advancement IDs
- **Feature gating**: milestone progress reads and writes are no-ops when
  `FeatureFlagCache.enableMilestones()` is false

### API

- **`MarieEvents.MilestoneTriggeredEvent`**: `getPlayer()`, `getMilestone()`, `getValueKey()`,
  `getCumulativeIntake()`

### Integrations

- **KubeJS `milestoneTriggered` event**: `MarieKubeEvents.MILESTONE_TRIGGERED` /
  `MarieMilestoneTriggeredEvent` exposes `playerId`, `milestoneId`, `valueKey`, `cumulativeIntake`,
  and `cumulativeGoal`; bridged through `KubeEventBridge` with `KubeGuard` listener short-circuit

### Notes

- Published artifact version is **0.1.0-beta.3** (`gradle.properties`)
- Milestones datapack loader from 1.0.0 is now fully wired (previously threw
  `UnsupportedOperationException` at parse time)

---

## [ MariesLib 0.1.0-beta.2 ] 2026-6-13

Stability, value-pipeline, death-handling, and KubeJS integration release.

### Added

- **`ValueModifierContext`**: structured modifier context (player, source, value key,
  level, position) shared by `ValueModifierEvent` and the value pipeline
- **`DeathNutritionBehavior`** (`@Stable`): `PRESERVE`, `RESET_TO_STARTING`, `VANILLA_HALF`;
  configured via `MarieLibContext.Builder.deathNutritionBehavior()` or a custom
  `deathTrackingTransformer`
- **`TrackingResetSupport`**: death respawn policy application, starting-fill resolution, and
  bar reset helpers used by commands and lifecycle handlers
- **`AttachmentTrackingDataProvider`**: ensures tracking data provider initialization in
  `MarieLibContext`
- **`SourceApplicationPipeline` direct writes**: `writeValue`, `finalizeValue`, and related
  helpers for command and integration code paths
- **Project documentation**: `API.md`, `ARCHITECTURE.md`, `PHILOSOPHY.md`, `RoadMap.md`
- **KubeJS package layout**: moved from `compat/kubejs/` to `kubejs/` with `bindings/`,
  `events/`, and `internal/` packages; `KubeIntegration` centralizes optional KubeJS wiring
- **KubeJS event bridges**: `MarieDecayTickEvent`, `MariePlayerSyncedEvent`,
  `MarieSourceConsumedEvent`, `MarieValueChangedEvent`, `MarieValueCriticalEvent`,
  `MarieValueDeltaModifierEvent`, `MarieValueExcessEvent`
- **EMI/REI service registration**: `META-INF/services` entries for recipe-viewer plugins

### Changed

- **`ValueModifierEvent`**: backed by `ValueModifierContext`; event constructor is
  `@Internal` (addon authors subscribe, they do not construct)
- **`MariePlayerCommands`**: `/value`, `/set`, and `/reset` route through
  `SourceApplicationPipeline` instead of ad-hoc tracking writes
- **Death and respawn handling**: `PlayerTrackingLifecycle` applies
  `DeathNutritionBehavior` on respawn; `TrackingAttachment` copies on death;
  `TrackingData` resolves initial bar fill through `TrackingResetSupport`
- **API stability pass**: broader `@Stable` coverage on events, definitions, registries,
  runtime resolver types, client toast tooling, and command helpers
- **`MarieAPI` validation**: rejects null definitions and invalid IDs during value
  registration; validates `valueKey` in `modifyValue`
- **Build reliability**: `createMinecraftArtifacts` reruns when the NeoForge compile jar is
  missing; task dependency ordering fixes in `build.gradle`
- **Assets and metadata**: banner/icon filenames normalized for case consistency; unused
  duplicate icon assets removed; `neoforge.mods.toml` dependency entries corrected

### Breaking Changes

- **KubeJS Java package move**: internal KubeJS classes relocated from
  `dev.marie.MariesLib.compat.kubejs` to `dev.marie.MariesLib.kubejs.*`; script-facing
  `MarieAPI` / `MarieKubeEvents` bindings are unchanged

### Notes

- Published artifact version is **0.1.0-beta.2** (`gradle.properties`)

---

## [ MariesLib 2.0.0 ] 2026-6-11

Library purification release. MariesLib is now a domain-agnostic infrastructure library.
All gameplay balance configuration has been removed from the library and moved to consuming mods.

### Breaking Changes

- **Config ownership**: All gameplay settings (memory, thresholds, decay, effects, modules, presets) removed from `config/marieslib.cfg`. Consuming mods must now own their balance configuration.
- **`ModuleCache` → `FeatureFlagCache`**: Consuming mods call `FeatureFlagCache.sync(MarieModFeatureFlags)` after config changes.
- **`IMarieLibConfig` → `MarieLibSettings`**: Library settings interface slimmed to scanner and debug only.
- **`TrackingMemoryConfig` → `DiminishingReturnsConfig`**: Renamed for clarity.
- **Handler renames**: `ValueDecayHandler` → `ValueDecayListener`, `ValueEffectsHandler` → `ValueEffectsListener`, `SleepBonusHandler` → `RestCycleListener`, `RecipeServerHandler` → `RecipeTriggerListener`, `TrackingPlayerEvents` → `PlayerTrackingLifecycle`, `ReloadHandler` → `ReloadGuardListener`, `HandlerSupport` → `DiminishingReturnsSupport`.
- **`ISourceTriggerHandler` → `SourceTriggerListener`**: Interface renamed.
- **Multi-mod registry**: `MarieModRegistry` replaces single-instance `MarieLibContext`.
- **Config screen**: Library screen shows Overview, Scanner, Diagnostics, and Tools tabs. Presets tab removed (owned by consuming mods).
- **Command namespace split**: All toolkit commands (`diagnostics`, `scan`, `scan_analysis`, `schema`, `reload`, `invalidatecache`, `repair_generated_datapack`, `debug cache/held/<player>`, `nbt`, `get_unassigned`, `report`, `value`, `set`, `reset`, `profile`) now register only under each consumer mod's namespace (e.g., `/nourished`). New library-only commands (`status`, `mods`, `api`, `registries`) are on `/marieslib` and `/marie`.
- **Presets/Import-Export**: Path resolution uses consuming mod's modId. Delete stale `config/marieslib/presets/` files.
- **Domain hardcoding removed**: No references to `nourished`, `nutrition`, `heavy_source`, `light_source` in library source.

### Added

- `MarieModRegistry`: multi-mod registration with `getAll()`, `getPrimary()`, `get(modId)`
- `MarieModFeatureFlags`: consuming mods supply feature flag snapshots
- `FeatureFlagCache`: hot-path feature flag reads
- `MarieLibSettings`: library-only settings interface (scanner + debug)
- Framework-only config tabs: Overview (read-only status), Scanner, Diagnostics, Tools (library command cheat sheet)
- `/marieslib` and `/marie` library-only commands: `status`, `mods`, `api`, `registries`
- All toolkit commands register under consumer mod namespace (e.g., `/nourished scan`, `/nourished diagnostics`)
- **Milestone progress tracking** (`MilestoneTracker`, `MilestoneProgressData`, `MilestoneProgressAttachment`): cumulative per-value intake, completion detection, and reward grants when milestones are enabled
- **`MarieEvents.MilestoneTriggeredEvent`**: fired when a player completes a milestone; bridged to KubeJS as `milestoneTriggered`
- **Datapack milestone loading** (`MarieDataLoader`, `MarieDatapackCallbacks`): milestone JSON parsing and registration into `MilestoneRegistry`
- **Datapack effect parsing** (`MarieDataLoader.parseEffect`): effect rewards loaded from milestone/datapack JSON
- **`MilestoneDefinition.advancementId`**: optional advancement grant on milestone completion; KubeJS `registerMilestone` accepts `advancementId`
- **`generate_milestone_template` command** (`MarieMilestoneTemplateCommand`): writes a starter milestone + advancement datapack to `<world>/datapacks/<modid>-milestone-template/`
- **`ValueDefinition.colorOverride`**: optional per-value ARGB override; `MarieValueColors` prefers definition override, then transient UI override, then palette index
- **Datapack validation**: `DatapackValidator` ignores `//` comment fields in JSON
- **Compile dependency pins** (`gradle.properties`, `build.gradle`): `jei_version`, `rei_version`, `emi_version` replace `+` ranges; `cloth-config-neoforge` forced to `${cloth_config_version}` so Java 21 builds do not resolve future Java 25 artifacts

### Migration Guide

1. Move all gameplay config (modules, memory, thresholds, decay, effects, presets) to your mod's own config file.
2. Implement `MarieLibContext.Builder.featureFlags(Supplier<MarieModFeatureFlags>)` and call `FeatureFlagCache.sync()` on load/save.
3. Register your own `configScreenFactory` with full Cloth Config categories for your mod's settings (including presets if desired).
4. Replace `IMarieLibConfig` references with `MarieLibSettings` (scanner/debug only) or `MarieLibContext` (gameplay methods).
5. Replace `ModuleCache.enableX` field accesses with `FeatureFlagCache.enableX()` method calls.
6. Delete stale `config/marieslib/presets/*.json` files.
7. Rename imports: `TrackingMemoryConfig` → `DiminishingReturnsConfig`, handler classes per above.
8. Update documentation: all toolkit commands (`diagnostics`, `scan`, `reload`, etc.) are now under your mod's namespace only (e.g., `/nourished diagnostics`). Use `/marieslib status` for library health.

### Notes

- Published artifact version is **0.1.0-beta.3** (`gradle.properties`)

---

## [ MariesLib 0.1.0-beta.2 ] 2026-6-10

Initial release. MariesLib is the shared infrastructure library extracted from Nourished so
Marie mods can depend on one backbone instead of duplicating registries, scanner tooling,
tracking, compat, and datapack loaders.

### Added

- **Standalone shared library** for Marie mods: required separate install on the classpath,
  no JarJar bundling. Consuming mods wire runtime through `MarieLibContext` at bootstrap.
- **Scanner pipeline**: bulk classification of every edible/source item in a modpack:
    - Token stemming (`TokenStemmer`), archetype patterns, and recipe inheritance
    - Spread-based confidence validation (not hard thresholds)
    - Tag recommendation writer: ready-to-paste datapack JSON under `data/<modid>/tags/item/`
    - Multi-value analysis: secondary groups, overlap matrices, ambiguous-item reports
    - Scan cache and metrics (`ScannerMetrics`, `CacheStats`)
- **Classification traces**: inspectable per-item pipeline decisions (`ClassificationTrace`,
  `ClassificationTraceStep`, `ClassificationTraceFormatter`):
    - Tag lookup, resolver scores, blend precedence, confidence spread
    - Runtime stages: SIGNAL_AGGREGATION, WINNER_SELECTION, CONFIDENCE, TAG_RUNTIME_BLEND
- **Runtime classification**: gameplay-path resolution separate from bulk scanner:
    - `RuntimeResolver` with cascade stages and resolution outcomes
    - Tag/runtime blending (`ValueBlend`) with precedence rules (TAG vs RUNTIME_SUPPLEMENT)
    - Source families (`FamilyResolver`) and `config/<modid>/source_overrides.json`
- **Tracking system**: full player value progression infrastructure:
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
- **Module system**: `ModuleCache` hot-path feature flags and server-side `LockRegistry`
  for datapack module locks
- **Config presets**: `PresetRegistry` with save/load/delete and compressed share-code
  import/export (`ImportExportManager`, Cloth Config widgets)
- **`/marie` commands**: `report`, `value`, `set`, `reset`, `profile`, `reload`,
  `invalidatecache`, `diagnostics`, `scan_analysis`, `schema`, `debug`
- **Client UX**: critical value toasts, preset cards, import/export screens, client
  tracking cache
- **Source application pipeline**: eating handlers, decay/effects handlers, reload
  pipeline, and tracking player events wired through `MarieLibContext`
- **`PresetRegistry` delegates**: `ensureBuiltInFilesOnDisk`, `applyPresetValues`, and
  `enableAllEffects` delegate through `MarieLibContext` so consuming mods own preset
  behavior without runtime crashes
- **Mod lifecycle**: `MariesLib` constructor accepts `ModContainer` for proper bootstrap
- **Mod icon**: `MariesLib_icon.png` for NeoForge/Modrinth metadata
- **`DeathNutritionBehavior`** (`PRESERVE`, `RESET_TO_STARTING`, `VANILLA_HALF`): server-side respawn policy for tracking bars and application memory via `MarieLibContext.deathNutritionBehavior()`
- **`TrackingResetSupport`**: applies the configured death behavior on respawn
- **`TrackingAttachment.copyOnDeath()`**: copies attachment state across death for consistent respawn handling
- **`PlayerTrackingLifecycle`**: respawn path delegates to death-behavior reset logic instead of always zeroing bars to 50%
- **`TrackingData`**: initial bar fill resolves through `DiminishingReturnsConfig.startingValueFill()`

### Architecture

- **`MarieLibContext` bootstrap**: consuming mod injects config suppliers, resolvers,
  screen factories, scanner callbacks, and preset delegates at startup
- **API lifecycle**: `MarieAPIState` registration phases; public types marked
  `@Stable`, `@Experimental`, or `@Internal` via `ApiStatus`
- **Registry framework**: `AbstractRegistry`, `RegistryLifecycleManager`,
  `RegistrySnapshot`; atomic reload with read/write locks during datapack reload
- **Handler pipeline**: source application → decay → effects → events, gated by
  `ModuleCache` flags and reload-in-progress state
- **Hot-path caching**: `ModuleCache` for tick/render loops; bounded LRU for scanner
  and resolver caches

### API

- **`MarieAPI`** (`@Stable`): static entry point for consuming mods and addons:
    - Queries: `getValueLevel`, `getTotal`, `getSourceMemory`, `getTrackingData`,
      `modifyValue`, `getVersion`
    - Registration: values, source classifications, effects, compat, synergies, profiles,
      milestones, season hooks, absorption modifiers, report providers
    - **`MarieEvents`** (`@Stable`): `ValueChangedEvent`, `ValueCriticalEvent`,
      `ValueExcessEvent`, `SourceAppliedEvent`
- **`ValueModifierEvent`**: cancellable pre-apply modifier hook
- **`MarieLibContext`** (`@Stable`): runtime context builder for bootstrap injection
- **`MarieLibRegistrationDelegate`**: value/effect/source registration contract
- **Experimental**: `ProfileDefinition`, `MilestoneDefinition`, `SynergyDefinition`,
  `SourcePairSynergy`, `AbsorptionModifier`, `MarieSeasonHook`, `ValueRenderer`,
  `ReportProvider`, and associated registries

### Integrations

- **KubeJS**: `MarieAPI` and `MarieEvents` script bindings via `kubejs.plugins.txt`
  and `META-INF/services` service loader (`MarieKubeJSPlugin`, `MarieKubeJSBindings`)
- **JEI / REI / EMI**: shared value tooltip helper (`MarieTooltipHelper`) for recipe
  viewers; all integrations are compileOnly optional dependencies
- **Cloth Config**: preset cards (`PresetsWidget`), import/export buttons
  (`ImportExportButtonsWidget`), save-preset screen (`SavePresetScreen`)

### Breaking Changes

- **`CompatDefinition` package move** (beta-period break, no deprecation shim):
    - **From:** `dev.maire.nourished.api.CompatDefinition`
    - **To:** `dev.marie.MariesLib.compat.CompatDefinition`
- **Tracking attachment schema reset**: legacy player tracking data from pre-extraction
  Nourished saves will not migrate; acceptable for the beta extraction period
- Future breaking API changes will include a deprecation shim and changelog notice

### Important Upgrade Notes

If upgrading from Nourished (or another Marie mod) before the MarieLib split:

1. Install **MariesLib 0.1.0-beta.3+** alongside any Marie mod that depends on it (e.g.
   [Nourished 0.2.5-beta.5+](https://modrinth.com/mod/nourished)).
2. Update imports: shared types (`CompatDefinition`, scanner types, tracking types,
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
- Network sync infrastructure is expanding: broader sync work remains on the roadmap
- First consuming mod: [Nourished](https://modrinth.com/mod/nourished) 0.2.5-beta.5+
- License: LGPL-3.0-only
- Published artifact version is **0.1.0-beta.2** (`gradle.properties`)

---

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
