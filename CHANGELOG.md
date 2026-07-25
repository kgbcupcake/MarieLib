# Changelog

<!-- markdownlint-disable MD013 -->

## [MariesLib 0.1.1-beta.4] — 2026-07-24

Scanner signal stages extracted into the `ResolutionStageHandler` cascade shape, `ComponentClassifier` wired into recipe-inheritance fallback, `DeathNutritionBehavior` renamed for domain-agnosticism, a marie-core/marie-ui/marie-commands audit for leftover food/nutrition-specific naming, tooltip message/color customization, excluded-item tooltip handling, a bundled-resource lookup fix, an excluded-items registry, and recipe-inheritance score merging for tagged items.

### Added

- `CommunityTagResolutionStage` / `SuffixResolutionStage` / `KeywordResolutionStage` (`dev.marie.framework.scanner.stages`)
  - Mechanical extractions of `ItemClassifier`'s former `analyzeSignal1CommunityTags` / `analyzeSignal3Suffix` / `analyzeSignal4Keywords` private methods into proper `ResolutionStageHandler` implementations
  - Same weight-table lookups and scoring math as before, unchanged output
- `CommunityTagResolutionStage.tagDirectory()`
  - Exposes the actual `c` namespace tag directory the stage scans, so callers never need to hardcode a duplicate copy of the value
- `RecipeInheritanceStage` now falls back to `ComponentClassifier.classify(...)` for a recipe ingredient when both `classifiedLookup` and its direct tag scores come back empty
- `RespawnValueBehavior` (`dev.marie.framework.tracking`)
  - Renamed from `DeathNutritionBehavior`; enum constants and config IDs (`preserve`, `reset_to_starting`, `vanilla_half`) unchanged
  - `MarieContext#respawnValueBehavior()` / `#respawnValueHandler()`
  - `MarieContext.Builder#respawnValueBehavior(Supplier)` / `#respawnValueHandler(BiConsumer)`
  - `TrackingResetSupport.applyRespawnValueBehavior(player, tracking)`
- `TooltipMessageRegistry` / `TooltipColorRegistry` (`dev.marie.framework.tooltips`)
  - Two-tier override lookup per `modId`: `config/<modId>/tooltips/tooltip_messages.json` / `tooltip_colors.json` (modpack-creator tier), overridden by `data/<modId>/marie/tooltips/tooltip_messages.json` / `tooltip_colors.json` (datapack tier)
  - Both files share a `byKey` / `byItem` shape; `byItem` takes priority over `byKey` for `getForItem(modId, itemId, key)`
  - `seedDefaultsIfAbsent(modId, defaults)`, `reload(modId)`, `loadFromDatapack(modId, resourceManager)`
- `MarieTooltipHelper` now checks `ExcludedItemsRegistry.isExcluded(itemId)` / `ScannerSpecRegistry.get().excludedItems()`
  - For excluded items, renders an `"excluded"`-keyed tooltip line/color sourced from `TooltipMessageRegistry` / `TooltipColorRegistry` instead of the usual unclassified-item line
- `ExcludedItemsRegistry` (`dev.marie.framework.scanner`), wired into `MarieBootstrap`'s lifecycle
- `ColorRegistry` / `ScannerSpecRegistry` now write a README (`COLORS_README.md`, `SCANNER_SPEC_README.md`) into the config folder on first load
  - Mirrors the pattern already used by `SourceClassificationRegistry` / `ExcludedItemsRegistry`
- `MarieComponent.mouseScrolled(double, double, double, double)` default method (no-op)
  - Forwarded unconditionally by `EditOverlayScreen` alongside the existing click/release/drag forwards
- `DoubleClickRecognizer` (`dev.marie.framework.ui.edit`)
  - Standalone per-button double-click gesture tracker (~300ms window, 4px tolerance)
  - Exposes separate left/right double-click callbacks for a component to wire into its own `mouseClicked` ahead of the normal `DraggableResizable.mouseClicked` forward
- `ComponentState.contentScale`
  - Optional, domain-agnostic scale for a component's content independently of its own box bounds (e.g. a zoomable inner element)
  - Defaults to `1.0`; `MarieConfigPersistenceProvider` reads/writes the new field with the same default fallback for existing persisted state that predates it
- `DraggableResizable(MarieComponent, Constraint, BiConsumer, Bounds)` constructor overload and `setParentBounds(Bounds)`
  - Clamps every subsequent drag/resize preview and commit to stay fully inside the given parent bounds, for a second tracker positioning a component's content within its own (separately tracked) box bounds
  - `null`/omitted parent bounds keeps the original unclamped behavior

### Changed

- `ItemClassifier.classify()`
  - Primary (non-fallback) scan path now runs community-tag/suffix/keyword scoring through the same `ResolutionStageHandler` mechanism as the runtime resolver cascade, instead of calling private methods directly; functionally unchanged
  - Stage instances are built once per `classify()` call and threaded through to `RecipeInheritanceStage.apply()` rather than re-instantiated per ingredient lookup
- `RecipeInheritanceStage.apply()` / `buildLookup()`
  - New `validKeys` / `componentFallbackStages` parameters to support the `ComponentClassifier` fallback
- `MarieTooltipHelper` moved from `dev.marie.framework.compat` to `dev.marie.framework.tooltips`
  - `MarieEmiPlugin` / `MarieReiPlugin` updated to the new import
- `SourceClassificationRegistry`'s override folder layout
  - `config/<modId>/overrides/source_classifications.json` moved under a dedicated `overrides/Overrides/` subfolder, with its README under `overrides/Read_Me/`
  - Existing flat-layout and legacy root-level `source_classifications.json` files are migrated/deleted automatically on load
- `SourceCollector` now merges qualifying recipe-inherited secondary scores into already-tagged items instead of skipping them outright
- `SourceApplicationPipeline.process()` no longer short-circuits when a `SourceClassificationRegistry` override is present
  - `ctx.sourceValueResolver()` / `ctx.sourceDeltaResolver()` are now always invoked and merged with the override (override wins per-key on conflict, resolver fills any gaps; `total` falls back to the resolver's computed delta when the override's `total` is 0/unset)
- `AutoGrowPanelContainer`'s footprint calculation simplified to respect a sibling's true committed size instead of also maxing against its natural height
- `ModuleRegistry` stays the static, singleton-facade shape shared by every other registry in this codebase (private constructor, static backing store) rather than becoming instantiable
  - Documented explicitly, since a prior design pass considered making it instantiable to support per-region module lists (e.g. a screen's left vs. right column)
  - Its `register`/`get` key parameter (previously documented only as "modId") is now documented as an opaque registry key: independent module lists are obtained by registering under distinct keys (conventionally `<modId>.<region>`, e.g. `"nourished.diet.right"`), not by constructing a second registry object
  - No behavioral change
- `@ApiStatus` tier corrections for classes that had real cross-mod consumers despite being marked (or left unmarked as) `Internal`
  - `MarieContext`, `ItemScanner`, `ModCompat`, `ScannerSpecRegistry`, `RecipeInheritanceResolver`, `TokenStemmer` (all marie-core) and `DraggableResizable` (marie-ui) are now `@ApiStatus.Experimental`
  - `MarieDataLoader` and `DraggableResizable` previously had no annotation at all; both now carry `@ApiStatus.Experimental` explicitly
  - `Internal` means "never referenced by any consuming mod" — these were all referenced by Nourished, so `Internal` was the wrong tier. No promotion to `Stable`; annotation-only change, no logic touched
- `RuntimeResolver` (`dev.marie.framework.runtime`) split
  - Cache-hit/miss counters, resolve-timing telemetry, and `computeSpread` extracted to a new package-private `RuntimeResolverStats`, held by composition
  - `RuntimeResolver` delegates to it internally; `getCacheStats()`, `invalidateCache()`, and `recordRecipeTimeout()` keep their existing public signatures and behavior unchanged
- `ItemClassifier` (`dev.marie.framework.scanner`) split
  - The namespace/negative-keyword/archetype/source-property/namespace-peer signal-analysis methods (formerly `analyzeSignal2/5/6/7/9`) extracted to a new package-private `ItemClassificationSignals`, called as static delegates from `classify(...)`
  - Contribution-scaling and result-building stay in `ItemClassifier` since they're shared across every signal, not signal-specific. No behavior change
- `ClassificationTraceFormatter` (`dev.marie.framework.classification`) split
  - The "Diagnostics" and "Developer Metadata" sections (`appendDiagnostics`, `appendDeveloperMetadata`, `collectDiagnostics`, the `DiagnosticEntry` record) extracted to a new package-private `ClassificationDiagnosticsFormatter`, called as static delegates from `format(...)`
  - Shared rendering helpers (`appendLine`, `appendKv`, `getString`, `collectSteps`, `SEP_HALF`) widened from `private` to package-private so the new class can reuse them instead of duplicating. No behavior change
- `MultiValueAnalysisWriter` (`dev.marie.framework.scanner.analysis`) trimmed, not split (already package-private with a single caller, so a class split wasn't warranted)
  - The repeated banner border/title block and `"Generated: " + timestamp` line, previously re-typed in every `write*Txt` method, are now `writeBanner(Writer, String)` / `writeGeneratedLine(Writer)` private helpers
  - Byte-identical output to before at every call site; no behavior change
- `MarieAPI` (`dev.marie.framework.api.marieapi`, 746 → 570 lines) split
  - Every public method's body moved into one of 13 new package-private delegate classes (`PlayerStateDelegate`, `ValueSourceRegistrationDelegate`, `EffectRegistrationDelegate`, `CompatRegistrationDelegate`, `SynergyRegistrationDelegate`, `ProfileMilestoneSeasonDelegate`, `ReportingRegistrationDelegate`, `ConfigValidationDelegate`, `TriggerRegistrationDelegate`, `TagAuditRegistrationDelegate`, `CommandCapabilityDelegate`, `SourceTriggerFiringDelegate`, `HookProviderRegistrationDelegate`), grouped by which registry/subsystem each method touches
  - Every `MarieAPI` public method keeps its exact signature, javadoc, `@ApiStatus` annotation, and declared exceptions and now just delegates in one line
  - No public API changes, no consumer call site is affected (verified against every `MarieAPI.register*`/`add*`/`get*` call in Nourished and Thermal_Systems). Pure internal reorganization; no behavior change
- `SourceApplicationPipeline` (`dev.marie.framework.handler`, 599 → 421 lines) split into 4 new package-private delegate classes
  - `SynergyStateRegistry` (per-player synergy cooldown/active-state maps, `clearPlayer`, `meetsSynergyCondition`)
  - `SourceApplyDebugReporter` (`submitSourceApplyDebug`, `buildMultiplierBreakdownJson`)
  - `ThresholdCrossingEvaluator` (`checkThresholdCrossings`, called from both `process()` and `writeDirectValue()`)
  - `ValueAbsorptionAdjuster` (`applySeasonalAbsorption`, `applyAbsorptionModifiers`)
  - `process()`'s synergy loops stay inline exactly as they were, now calling into `SynergyStateRegistry` instead of touching raw maps directly; `process()`'s own structure and the override/resolver merge logic are untouched
  - Every existing public method (`process`, `clearPlayer`, `writeDirectValue`, `applyDirectDelta`, `finalizeDirectWrite`, `resetSnapshotWarnings`) keeps its exact signature — verified against all 7 real callers (`SourceTriggerFiringDelegate`, `ValueDecayListener`, `AttachmentTrackingDataProvider`, `TrackingResetSupport`, `MarieCommandSupport`, `MarieKubeServerBindings`, `MariePlayerCommands`)
  - Confirmed `@ApiStatus.Internal` remains accurate: no consumer mod (Nourished, Thermal_Systems, ThinAir-ReLived) references this class directly. Pure internal reorganization; no behavior change

### Deprecated

- `MarieContext#deathNutritionBehavior()` / `#deathNutritionHandler()`
  - Use `#respawnValueBehavior()` / `#respawnValueHandler()`; old accessors now forward to the new ones
- `MarieContext.Builder#deathNutritionBehavior(Supplier)` / `#deathNutritionHandler(BiConsumer)`
  - Use `#respawnValueBehavior(Supplier)` / `#respawnValueHandler(BiConsumer)`; old builder methods now forward to the new ones

### Fixed

- `ItemClassifier`'s `COMMUNITY_TAG` classification signal label
  - Was a hardcoded `"c:foods/*"` literal left over from a mechanical extraction; now derived from `CommunityTagResolutionStage.tagDirectory()`
- Domain-agnostic audit: removed leftover food/nutrition-specific wording from marie-core comments (`MilestoneRegistry`, `DatapackSchema`, `SourceClassificationRegistry`, `TagAuditContext`) and from marie-ui (`MarieValueColors`) and marie-commands (`MarieMilestoneTemplateCommand`'s user-facing `_comment_value_key` output)
- `ScannerSpecRegistry.writeBundledTo` now uses the context classloader (matching `parseBundled`), so the bundled `scanner_spec.json` resolves correctly across module boundaries
- `SourceClassificationRegistry.parseEntry()` no longer throws an unguarded `NullPointerException` when an entry omits `source_id` (or any array element isn't a JSON object)
  - Malformed entries are now logged and skipped individually (by `source_id` if readable, otherwise by array index) instead of aborting the whole file and crashing mod bootstrap
  - `load()` also gained a broader catch as a second line of defense against whole-file corruption (invalid JSON syntax, non-array top-level value)
- Removed `TOOLTIP_COLORS_README.md` / `TOOLTIP_MESSAGES_README.md` from marie-ui's bundled resources
  - `writeReadmeIfAbsent` looks up `data/<modId>/config/...` using the *consuming* mod's runtime `modId`, so a copy bundled under marie-ui's own `marieslib` namespace was never reachable by any real consumer, same bug class as `SOURCE_CLASSIFICATIONS_README`
  - Consumer mods must now bundle their own copy under their own `data/<modid>/config/` namespace
- Removed `COLORS_README.md` / `SCANNER_SPEC_README.md` from marie-core's bundled resources for the same reason
  - `ColorRegistry`/`ScannerSpecRegistry`'s `writeReadmeIfAbsent` resolve `data/<modId>/config/...` by the consuming mod's runtime `modId`, so the copies bundled under marie-core's own `marieslib` namespace were never reachable
  - Only affects the READMEs — `ScannerSpecRegistry`'s bundled `scanner_spec.json` *defaults* use a separate, already-correct resolution path (`data/<modId>/<modId>/scanner/scanner_spec.json`, supplied by each consumer) and were not touched
- Removed dead-code `dev.marie.framework.registry.ExportResolverRegistry` (and its nested `Core`/`Entry` types)
  - Zero callers anywhere in the codebase, and not wired into `MarieApiRegistries`'s freeze/reset lifecycle like every other registry in that package
  - Its `AbstractRegistry`-based frozen/locked design predates and directly contradicts the shipped `dev.marie.framework.export.ExportResolverRegistry` (0.1.1-beta.1), which is documented as never frozen so resolvers can register throughout mod init — an abandoned false start, not an in-progress migration. `export.ExportResolverRegistry` is untouched
  - `TagRuleRegistry` / `TagAuditContextRegistry` Javadoc comments that referenced "ExportResolverRegistry" by bare name now name `export.ExportResolverRegistry` explicitly, now that only one class exists
- `MarieAPI.registerExportResolver(ExportResolver<T>)` (single-arg overload) no longer silently registers a permanently-null export
  - It previously called `ExportResolverRegistry.register(resolver.resolverId(), () -> null)`, discarding the passed-in `resolver` entirely — any dump run through this overload would always produce nothing
  - Root cause: `ExportResolver<T>` carries no registry key, so this overload structurally has no way to know which registry to iterate (unlike the two-arg overload, which takes a `ResourceKey<Registry<T>>` explicitly) — there is no correct implementation for it, not just a wiring mistake
  - Confirmed via grep that no real consumer (Nourished, Thermal_Systems, ThinAir-ReLived) has ever called this overload — Nourished's only `registerExportResolver` call site uses the two-arg form — so this was dead code from the moment it was introduced, not a regression that silently broke a working export
  - The delegate body (now in `HookProviderRegistrationDelegate`) throws `UnsupportedOperationException` instead of silently no-oping, and the method is marked `@Deprecated` with a `@deprecated` javadoc pointing callers at the two-arg overload. No signature change; both overloads keep their exact public shape

### Notes

- Version: **0.1.1-beta.4**
- `TrackingResetSupport.applyDeathNutritionOnRespawn` (internal-only, `@ApiStatus.Internal`) was renamed directly to `applyRespawnValueBehavior` with no deprecated forwarder, since it isn't public API
- Flagged but intentionally untouched: `CommunityTagResolutionStage`'s underlying `"foods/"` NeoForge tag directory value (real scoring behavior, not a naming artifact) and marie-ui's javadoc comments documenting generalization from Nourished's original screens (attribution, not a leak)

---

## [MariesLib 0.1.1-beta.3] — 2026-06-30

Source-pair synergy value buffs, unified source classification, tag-audit/export command wiring, and player logout cleanup for synergy state.

### Added

- `MarieAPI.registerConfigValidator(validator)` now validates `modId()` (must be non-empty and registered); throws `IllegalArgumentException` otherwise
- `SourcePairSynergy.getValueModifier()` / `getModifierDurationTicks()` (`@ApiStatus.Stable`)
- Builder: `valueModifier(float)`, `modifierDurationTicks(int)`
- Temporary value multiplier applied when synergy fires
- `SynergyBuffTracker` (tracking)
- Per-player, per-value-key temporary modifier store
- `activate(playerId, valueKey, modifier, expiryTick)`
- `getActiveModifier(playerId, valueKey, currentTick)` (auto-expire)
- `clearPlayer(playerId)`
- `SynergyAbsorptionModifier` (`AbsorptionModifier`)
- Applies `SynergyBuffTracker` modifiers during absorption
- Registered via `MariesLibBootstrap`
- `SourceApplicationPipeline.clearPlayer(playerId)`
- Clears synergy runtime state on logout
- Wired into `PlayerLoggedOutEvent`
- Datapack schema:
- `value_modifier`
- `modifier_duration_ticks`
- `MarieDataLoader.parseSynergy` / `parseSourcePairSynergy`
- Fully implemented datapack loading for synergies
- `CommandCapability` / `CommandCapabilityRegistry` (`@ApiStatus.Experimental`)
- Mod-registered command extensions keyed by `(modId, capability)`
- `ExportWriter.writeExport(resolverId)` / `RegistryExporter`
- Writes `config/<modid>/<resolverId>_export.json`
- Exposed via `/marieslib dump <resolverId>`
- `TagAuditReportWriter`
- Writes full tag audit reports to disk
- Hooked into `/marieslib audit_tags <modid>`
- `MariesLibCommand.runDump` / `runAuditTags`
- `SourceClassificationRegistry` rewrite
- Config-backed registry replacing `SourceOverrideRegistry` + `SourceValueRegistry`
- Supports migration from legacy files
- `RuntimeResolver` trace enhancement
- Adds `EXTERNAL_CLASSIFICATION` trace step when overrides apply

### Changed

- Removed `SourceOverrideRegistry` and `SourceValueRegistry`
- Fully merged into `SourceClassificationRegistry`
- `MarieAPI` reorganized (no behavior change)
- Registration methods regrouped (export, validators, audit, commands)
- `MariesLib` constructor
- No longer auto-bootstraps `MariesLibBootstrap`
- Consumers must explicitly bootstrap
- `RecipeInheritanceResolver.buildIndex`
- Now actually builds recipe index
- `PresetRegistry.PresetValues`
- Replaced fixed schema with raw `JsonObject values`
- `MarieValueColors.resolvedDefaultArgb`
- Now prioritizes definition override → palette → base color
- `ModCompat` resource loading
- Switched to context classloader-first lookup
- `ScannerSpecRegistry.parseBundled`
- Uses context classloader for mod JAR compatibility

### Fixed

- `ColorRegistry.parseArgbString`
- Fixed `0x` parsing inconsistency using `Long.parseLong`
- `MarieDebugCommand` trace dump overwrite bug
- Now writes unique timestamped files per run

### Notes

- Version: **0.1.1-beta.3**

---

## [MariesLib 0.1.1-beta.2] — 2026-06-27

Config validation, tag auditing system, curve math utilities, and scanner spec resilience improvements.

### Added

- `ConfigValidator` (`@ApiStatus.Stable`)
- `ConfigValidatorRegistry` (`@ApiStatus.Internal`)
- `Finding` (validation issue model)
- `ValidationResult` (PASS / WARN / FAIL + findings)
- `ValidationRunner.runForMod(modId)`
- Tag audit system:
- `TagAuditContext`
- `TagRule`
- `TagIssue`
- `TagFixSuggestion`
- `TagAuditSeverity`
- `TagReport`
- `TagScanner.scan(context)`
- `TagRuleRegistry`
- `TagAuditContextRegistry`
- `CurveGrid`
- 2D bilinear interpolation over normalized axes
- `CurveGridJson`
- `SourceClassificationRegistry` (read-only external view)
- `MarieAPI.registerConfigValidator`
- `MarieAPI.registerExportResolver` (overloads)
- `MarieAPI.registerTagRule`
- `MarieAPI.registerTagAuditContext`
- `/set_all <value> <player>` command
- `/validate` command
- `/analyze <item>` command
- `MarieValidationCommands`
- `SourceRegistry.getAllExternalClassifications`

### Changed

- `MariesLibCommand`
- Removed duplicate command tree registration
- `RecipeInheritanceResolver.getIngredients()`
- Made public; improved index construction
- `MarieValueColors`
- Expanded accessors for audit/export tooling

### Fixed

- `ScannerSpecRegistry.writeBundledTo`
- Now returns boolean
- Eliminates false warnings for missing bundled specs

### Notes

- Version: **0.1.1-beta.2**

---

## [MariesLib 0.1.1-beta.1] — 2026-06-20

Registry export framework, scanner fix, explicit bootstrap requirement, and registry lifecycle exposure.

### Added

- `ExportResolver<T>`
- `ExportResolverRegistry`
- `RegistryExporter`
- `ExportWriter.writeExport`
- `/marieslib dump <resolverId>`
- `/marie dump <resolverId>`
- `MarieAPI.registerExportResolver`
- `ValueRegistry.isFrozen()`

### Changed

- `MariesLib` constructor
- Removed implicit bootstrap behavior
- Requires explicit `MariesLibBootstrap.attach/bootstrap`

### Fixed

- `ScannerSpecRegistry` resource loading
- Switched to context classloader for mod JAR compatibility

### Notes

- Version: **0.1.1-beta.1**

---

## [MariesLib 0.1.0-beta.5] — 2026-06-16

Color resolution fixes and decay/config correctness improvements.

### Added

- `MarieValueColors.resolvedDefaultArgb`
- `IMarieLibConfig.decayRateFor`
- `MarieLibContext.Builder.decayRateFor`

### Fixed

- `ColorRegistry.parseArgbString` (`0x` handling fixed)
- Decay system now respects config overrides
- Commands now use config-based decay values
- API classification persistence fixed across reloads
- Critical toast fallback label fixed

### Notes

- Version: **0.1.0-beta.5**

---

## [MariesLib 0.1.0-beta.4] — 2026-06-16

Milestone system expansion, datapack effects, and tooling improvements.

### Added

- `ValueDefinition.colorOverride`
- `MilestoneRegistry.getForAll()`
- Cross-nutrient milestone tracking (`"all"`)
- `generate_milestone_template` command
- `MarieDataLoader.parseEffect`
- `SourceRegistry.clearSessionWarnings`

### Changed

- Datapack validator ignores `_comment_` fields
- Registration logging reduced/noise control improved
- Dependency pins fixed for JEI/REI/EMI
- CI workflow updated

### Notes

- Version: **0.1.0-beta.4**

---

## [MariesLib 0.1.0-beta.3] — 2026-06-15

Milestone tracking system implementation.

### Added

- `MilestoneTriggeredEvent`
- `MilestoneProgressData`
- `MilestoneProgressAttachment`
- `MilestoneTracker`
- `MarieDatapackCallbacks`
- Datapack milestone loader
- Bootstrap milestone wiring
- KubeJS milestone binding (`milestoneTriggered`)

### Architecture

- Pipeline hook: `SourceApplicationPipeline → MilestoneTracker`
- Reward system: effects + advancements
- Feature flag gating for milestones

### Notes

- Version: **0.1.0-beta.3**

---

## [MariesLib 0.1.0-beta.2] — 2026-06-13

Core stability, tracking pipeline, and KubeJS integration.

### Added

- `ValueModifierContext`
- `DeathNutritionBehavior`
- `TrackingResetSupport`
- `AttachmentTrackingDataProvider`
- `SourceApplicationPipeline` direct write APIs
- Full KubeJS event bridge set
- EMI/REI services

### Changed

- `ValueModifierEvent` refactored to context-based model
- Player lifecycle now uses `DeathNutritionBehavior`
- API stability annotations expanded
- Commands refactored to pipeline-based writes
- Build system improvements

### Breaking Changes

- KubeJS internal package relocation (non-breaking for scripts)

### Notes

- Version: **0.1.0-beta.2**

---

## [MariesLib 2.0.0] — 2026-06-11

Library purification release (domain-agnostic extraction from Nourished).

### Breaking Changes

- Removed all gameplay config ownership
- Replaced `ModuleCache` → `FeatureFlagCache`
- `IMarieLibConfig` → `MarieLibSettings`
- Handler renames across pipeline
- Command namespace split (library vs consumer mods)
- Preset system fully moved to consuming mods

### Added

- `MarieModRegistry`
- `MarieModFeatureFlags`
- `FeatureFlagCache`
- Library-only `/marieslib` commands
- Multi-mod registry architecture
- Scanner + tracking infrastructure foundation
- Milestone tracking system foundation
- Datapack loaders (values/effects/synergies/milestones)
- Registry framework rewrite
- Client UX tooling (toasts, preset UI, etc.)

### Migration Notes

- Move all gameplay config to consuming mods
- Implement feature flag syncing via `FeatureFlagCache`
- Delete legacy preset files
- Replace `ModuleCache` calls with feature flag API

### Notes

- Version: **2.0.0**

---

## [MariesLib 0.1.0-beta.2] — 2026-06-10

Initial extraction beta.

### Added

- Core shared library bootstrap
- Scanner pipeline system
- Classification trace system
- Runtime resolver system
- Tracking system (decay, synergy, milestones)
- Compat system (3-tier resolution)
- Datapack loaders (partial)
- Module system (`ModuleCache`)
- Preset system (Cloth Config integration)
- Full `/marie` command suite
- Client UI (HUD, toasts, config screens)
- Source application pipeline
- Mod lifecycle integration (`MarieLibContext`)
- KubeJS + JEI/REI/EMI integrations

### Architecture

- Registry framework with snapshots + reload locks
- Handler pipeline: source → decay → effects → events
- Hot-path caching systems
- Context-based bootstrap model

### Notes

- First stable beta extraction baseline
- Version: **0.1.0-beta.2**

---

## [MariesLib 0.1.0-beta.1] — 2026-06-10

First beta release.

### Added

- Standalone bootstrap system
- Cloth Config integration
- Source trigger pipeline
- Value scaling system (`amountScale`)
- Debug tooling
- Import/export system
- Full handler audit hardening
- Lazy datapack loading system
- Registry safety checks

### Architecture

- `MarieLibContext` bootstrap model
- Handler-based pipeline system
- Client/server separation enforcement

### Notes

- Version: **0.1.0-beta.1**
