# Changelog

<!-- markdownlint-disable MD013 -->

## [Unreleased]

Tooltip message/color customization (`dev.marie.framework.tooltips`), excluded-item tooltip handling, bundled-resource lookup fix, an excluded-items registry, and recipe-inheritance score merging for tagged items.

### Added

- `TooltipMessageRegistry` / `TooltipColorRegistry` (`dev.marie.framework.tooltips`)
  - Two-tier override lookup per `modId`: `config/<modId>/tooltips/tooltip_messages.json` / `tooltip_colors.json` (modpack-creator tier), overridden by `data/<modId>/marie/tooltips/tooltip_messages.json` / `tooltip_colors.json` (datapack tier)
  - Both files share a `byKey` / `byItem` shape; `byItem` takes priority over `byKey` for `getForItem(modId, itemId, key)`
  - `seedDefaultsIfAbsent(modId, defaults)`, `reload(modId)`, `loadFromDatapack(modId, resourceManager)`
- `MarieTooltipHelper` now checks `ExcludedItemsRegistry.isExcluded(itemId)` / `ScannerSpecRegistry.get().excludedItems()` and, for excluded items, renders an `"excluded"`-keyed tooltip line/color sourced from `TooltipMessageRegistry` / `TooltipColorRegistry` instead of the usual unclassified-item line
- `ExcludedItemsRegistry` (`dev.marie.framework.scanner`), wired into `MarieBootstrap`'s lifecycle
- `ColorRegistry` / `ScannerSpecRegistry` now bundle a README (`COLORS_README.md`, `SCANNER_SPEC_README.md`) into the config folder on first load, mirroring the pattern already used by `SourceClassificationRegistry` / `ExcludedItemsRegistry`

### Changed

- `MarieTooltipHelper` moved from `dev.marie.framework.compat` to `dev.marie.framework.tooltips`; `MarieEmiPlugin` / `MarieReiPlugin` updated to the new import
- `SourceClassificationRegistry`'s override folder layout: `config/<modId>/overrides/source_classifications.json` moved under a dedicated `overrides/Overrides/` subfolder, with its README under `overrides/Read_Me/`; existing flat-layout and legacy root-level `source_classifications.json` files are migrated/deleted automatically on load
- `SourceCollector` now merges qualifying recipe-inherited secondary scores into already-tagged items instead of skipping them outright
- `SourceApplicationPipeline.process()` no longer short-circuits when a `SourceClassificationRegistry` override is present: `ctx.sourceValueResolver()` / `ctx.sourceDeltaResolver()` are now always invoked and merged with the override (override wins per-key on conflict, resolver fills any gaps; `total` falls back to the resolver's computed delta when the override's `total` is 0/unset)
- `AutoGrowPanelContainer`'s footprint calculation simplified to respect a sibling's true committed size instead of also maxing against its natural height

### Fixed

- `ScannerSpecRegistry.writeBundledTo` now uses the context classloader (matching `parseBundled`), so the bundled `scanner_spec.json` resolves correctly across module boundaries
- `SourceClassificationRegistry.parseEntry()` no longer throws an unguarded `NullPointerException` when an entry omits `source_id` (or any array element isn't a JSON object); malformed entries are now logged and skipped individually (by `source_id` if readable, otherwise by array index) instead of aborting the whole file and crashing mod bootstrap. `load()` also gained a broader catch as a second line of defense against whole-file corruption (invalid JSON syntax, non-array top-level value)
- Removed `TOOLTIP_COLORS_README.md` / `TOOLTIP_MESSAGES_README.md` from marie-ui's bundled resources: `writeReadmeIfAbsent` looks up `data/<modId>/config/...` using the *consuming* mod's runtime `modId`, so a copy bundled under marie-ui's own `marieslib` namespace was never reachable by any real consumer, same bug class as `SOURCE_CLASSIFICATIONS_README`. Consumer mods must now bundle their own copy under their own `data/<modid>/config/` namespace.

---

## [MariesLib 0.1.1-beta.4] — 2026-07-13

Scanner signal stages extracted into the `ResolutionStageHandler` cascade shape, `ComponentClassifier` wired into recipe-inheritance fallback, `DeathNutritionBehavior` renamed for domain-agnosticism, and a marie-core/marie-ui/marie-commands audit for leftover food/nutrition-specific naming.

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

### Changed

- `ItemClassifier.classify()`
- Primary (non-fallback) scan path now runs community-tag/suffix/keyword scoring through the same `ResolutionStageHandler` mechanism as the runtime resolver cascade, instead of calling private methods directly; functionally unchanged
- Stage instances are built once per `classify()` call and threaded through to `RecipeInheritanceStage.apply()` rather than re-instantiated per ingredient lookup
- `RecipeInheritanceStage.apply()` / `buildLookup()`
- New `validKeys` / `componentFallbackStages` parameters to support the `ComponentClassifier` fallback

### Deprecated

- `MarieContext#deathNutritionBehavior()` / `#deathNutritionHandler()`
- Use `#respawnValueBehavior()` / `#respawnValueHandler()`; old accessors now forward to the new ones
- `MarieContext.Builder#deathNutritionBehavior(Supplier)` / `#deathNutritionHandler(BiConsumer)`
- Use `#respawnValueBehavior(Supplier)` / `#respawnValueHandler(BiConsumer)`; old builder methods now forward to the new ones

### Fixed

- `ItemClassifier`'s `COMMUNITY_TAG` classification signal label
- Was a hardcoded `"c:foods/*"` literal left over from a mechanical extraction; now derived from `CommunityTagResolutionStage.tagDirectory()`
- Domain-agnostic audit: removed leftover food/nutrition-specific wording from marie-core comments (`MilestoneRegistry`, `DatapackSchema`, `SourceClassificationRegistry`, `TagAuditContext`) and from marie-ui (`MarieValueColors`) and marie-commands (`MarieMilestoneTemplateCommand`'s user-facing `_comment_value_key` output)

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
