# Changelog

<!-- markdownlint-disable MD013 -->

## [Unreleased]

### Added

- `MarieTracking` (`dev.marie.framework.tracking.tracker`, marie-core) — self-contained public facade for the generic tracker/period-history subsystem, mirroring `MarieColors`'s pattern exactly (registration-phase guard via `MarieAPIState.assertRegistrationAllowed`, zero footprint in `MarieAPI`). `registerTracker`/`incrementTracker`/`getCurrentTrackerValue`/`getTrackerHistory` replace the old `MarieAPI` methods of the same names one-for-one. **Breaking change**: `MarieAPI.registerTracker`/`incrementTracker`/`getCurrentTrackerValue`/`getTrackerHistory` and the internal `TrackerRegistrationDelegate` are removed entirely — same precedent as the earlier `MarieAPI`-delegate-to-`MarieColors` rework for the color system; this is the second and (for now) last such move. Consuming mods must switch call sites to `MarieTracking`.
- Two-tier client sync for tracker state, fixing the client-side correctness gap `MarieAPI.getCurrentTrackerValue` previously had no way to close (attachments are server-authoritative and were never kept live-synced to the client, so a client-side call silently read stale/default data):
    - High-frequency path: `MarieTracking.incrementTracker` marks the touched tracker dirty per-player (new internal `TrackerDirtyState`); `TrackerManager.sweepDirtySync`, piggybacked onto the same per-player tick loop as `checkTrackers` (`ValueDecayListener.onPlayerTick`), pushes only the dirty trackers' current values to that player once at least `IMarieConfig#trackerSyncIntervalTicks()` (new config value, default `20`) ticks have passed since their last sync, then clears the dirty set. Carried by new `TrackerLiveSyncPayload` (`dev.marie.framework.tracking.tracker.network`, server→client, per-player, dirty-only — not `GenericStateSyncPayload`/`GenericConfigSyncPayload`, neither of which fit this shape).
    - Low-frequency path: on player login, `PlayerTrackingLifecycle.onPlayerJoin` sends every registered tracker's full history + period state via new `TrackerHistorySyncPayload`; on period completion, `TrackerManager`'s existing period-close logic sends a targeted single-tracker resync to just that player (not a broadcast). marie-core cannot depend on marie-resources per the locked one-directional module graph, so this does not reuse `MarieResourcesNetworking`'s per-player snapshot primitive — it's a marie-core-owned equivalent (`TrackerNetworking`, same `registrar.playToClient(...)` pattern).
    - `ClientTrackerCache` (`dev.marie.framework.tracking.tracker`, marie-core) — client-side-only transient keyed cache for current values, history, and period state, mirroring `ColorPreviewOverrides`'s shape, populated by `TrackerNetworking`'s payload handlers.
    - `MarieTracking.getCurrentTrackerValue`/`getTrackerHistory` now branch on side: a `ServerPlayer` reads the live server attachment as before; any other `Player` instance (client-side) reads `ClientTrackerCache` instead of the attachment.
    - New config surface: `IMarieConfig`/`MarieContext`/`FallbackConfig#trackerSyncIntervalTicks()` (default `20`), following the same `Supplier<Integer>` pattern as `trackerMaxRetention`/`trackerWeeklyPeriodDays`.
    - Opt-in immediate-sync mode alongside the throttled dirty-flag sweep: `TrackerDefinition.Builder#immediateSync(boolean)` (default `false`, reachable only through the full builder — not exposed via the `daily`/`weekly`/`monthly` convenience factories). When set, `MarieTracking.incrementTracker` sends that tracker's updated value to the client immediately via `TrackerNetworking.sendLiveValues`, bypassing the dirty-flag mark and throttled sweep entirely for that tracker. Intended for low-frequency, latency-sensitive trackers, not high-frequency ones — the existing `MarieNetworking` rate limiter (20 packets/sec per player) still applies and will silently drop excess traffic if misused.

- Generic server→client config/registry sync mechanism (`dev.marie.framework.resources`, marie-resources), replacing the need for consuming mods to hand-roll their own full-state sync payload/channel/login-handler per registry (this is what Nourished's `SyncNourishedConfigSnapshot`/`NourishedSyncHandler` did before):
    - `GenericConfigSyncPayload` (`dev.marie.framework.resources.network`) — `CustomPacketPayload` carrying a registry id plus an opaque `CompoundTag`, sent server→client via `registrar.playToClient(...)`
    - `MarieResourcesAPI` (`dev.marie.framework.resources.api`) — the module's public facade: `registerConfigSyncSupplier`/`registerConfigSyncClientHandler` register the server-side snapshot builder and client-side apply function for a registry id; `broadcastConfigSyncReload` rebuilds and resends one registry's snapshot to all connected players; `getConfigSyncState` (client-side) reports `SyncState.UNINITIALIZED`/`PENDING`/`ACTIVE` per registry id
    - On player login, every registered registry's snapshot is built and sent automatically; unknown/unregistered incoming registry ids on the client are logged and ignored, not thrown
    - Self-registers its `RegisterPayloadHandlersEvent` listener and `PlayerEvent.PlayerLoggedInEvent` hook via `@EventBusSubscriber(modid = MarieCore.MOD_ID)`, so marie-core never references marie-resources directly, per the locked one-directional module graph
- `GameplayTriggerListener` (`dev.marie.framework.handler`, marie-core) wires real detection into the previously-dormant `ValueSourceTrigger.TriggerType.BLOCK_BROKEN` and `ENTITY_KILLED` cases — `BlockEvent.BreakEvent` and player-caused `LivingDeathEvent`s now fire `MarieAPI.fireSourceTrigger` directly. Detection only; MarieLib still has no opinion on what these triggers mean.
- `ValueEffectsListener.onPlayerTick` (marie-core) now also fires `ValueSourceTrigger.TriggerType.TICK` (via `ValueSourceTrigger.tick("sprint")` / `tick("swim")`) whenever a player is sprinting or swimming, reusing the existing `APPLY_INTERVAL_TICKS` gating rather than adding a new listener.
- Generic tracker/period-history framework (`dev.marie.framework.tracking.tracker` and `.tracker.definition`/`.tracker.registry`, marie-core) — MarieLib manages per-player accumulators, period boundaries, and bounded history for arbitrary trackers with zero domain knowledge (no calories, no nutrients):
    - `TrackerDefinition` (`.tracker.definition`) — id, `TrackerPeriod` (`DAILY`/`WEEKLY`/`MONTHLY`/`REAL_TIME`/`SESSION`/`CUSTOM`), and retention (validated at registration, not construction); `daily(id, retention)`/`weekly(id, retention)`/`monthly(id, retention)` factories plus a full builder for `REAL_TIME`/`CUSTOM`
    - `TrackerHistoryEntry` (`.tracker.definition`) — immutable record of one completed period (tracker id, period id, start/end boundary, accumulated value)
    - `TrackerRegistry` (`.tracker.registry`) — enforces `IMarieConfig#trackerMaxRetention()` at `register()` time, throwing `IllegalArgumentException` if retention is out of bounds
    - `TrackerManager` (`.tracker`) — `checkTrackers(player, tracking)` runs once per player tick (from `ValueDecayListener`, independent of the decay feature flag) to open/close `DAILY`/`WEEKLY`/`MONTHLY`/`REAL_TIME` periods; `SESSION` trackers open/close once per login from `PlayerTrackingLifecycle.onPlayerJoin`; `CUSTOM` trackers close only via `fireCustomTrackerReset`
    - `MarieAPI.registerTracker` / `incrementTracker` / `getCurrentTrackerValue` / `getTrackerHistory` — the public registration/read surface, backed by a new `TrackerRegistrationDelegate`
    - `TrackingData` gained `trackingAccumulators`/`trackingHistory`/`trackerPeriodStates` maps, kept separate from the existing `values`/`total` nutrient-bar state, wired into the codec and `copySnapshot` the same way as `sourceMemory`/`categoryMemory`
    - New config surface on `IMarieConfig`/`MarieContext`/`FallbackConfig`: `trackerSystemEnabled()` (default `true`), `trackerMaxRetention()` (default `90`), `trackerWeeklyPeriodDays()` (default `7`), `trackerMonthlyPeriodDays()` (default `30`); `MarieContext.Builder.onTrackerPeriodCompleted(...)` hook fires once per boundary crossing
- Color system definition/identity/resolve layer, flat in `dev.marie.framework.color` (marie-core), completing the bridge between `ColorRegistry`'s existing override storage and the rest of MarieLib — `ColorRegistry` itself is unchanged, this is purely additive:
    - `ColorKey` — `record` wrapping a `ResourceLocation` identity; `ColorKey.of(id)` factory
    - `ColorDefinition` — a `ColorKey` plus its default packed-ARGB value; `ColorDefinition.of(key, defaultArgb)`
    - `ColorDefinitionRegistry` — `AbstractRegistry<ColorKey, ColorDefinition>` wrapper mirroring `MilestoneRegistry`'s shape (`freezeInternal`/`resetInternal`/`register`/`get`/`getAll`)
    - `MarieColors` — a self-contained public facade for this subsystem, separate from `MarieAPI`. `MarieColors.registerColor(ColorDefinition)` is gated by the same registration-phase check as every other MarieLib registration (`MarieAPIState.assertRegistrationAllowed`). `MarieColors.resolveColor(ColorKey)` checks `ColorRegistry` (keyed by `key.id().toString()`, the fixed `ColorKey`↔`ColorRegistry` mapping) for a user/datapack override first, falls back to the registered `ColorDefinition`'s default, and falls back to a logged-warning magenta error color if the key was never registered at all
- `ColorHexRowWidget` (`dev.marie.framework.client.config.cloth`, marie-ui) — a generic Cloth Config color row (swatch, `#RRGGBB` hex field, reset, live preview) built on `MarieColors`, generalizing the swatch/hex/reset/preview behavior of Nourished's `NutrientHudHexColorRowEntry` without that entry's nutrient-specific default-resolution logic. `NutrientHudHexColorRowEntry` itself is untouched; migrating Nourished onto the generic widget is a separate follow-up.
- `ColorPreviewOverrides` (`dev.marie.framework.color`, marie-core) — transient, non-persisted `ColorKey`→ARGB overrides, mirroring the string-keyed transient override map `MarieValueColors` already used for nutrient values, generalized to `ColorKey`. `ColorHexRowWidget` now pushes the in-progress (unsaved) hex value here on every keystroke and clears it on reset, so its own swatch preview reflects an edit live instead of only after Save+reopen. `ColorRegistry`'s own persistence is unchanged — it's still only written on `save()`.
- `ColorHexRowWidget.syncFromEffectiveColor()` — re-reads the current effective color (`ColorRegistry` override, else default via `MarieColors.resolveColor`) and updates the row's own swatch/hex field in place, without rebuilding the containing screen. Lets a bulk "reset all" flow refresh every visible row directly instead of reopening the config screen, mirroring the old `NutrientHudHexColorRowEntry.syncAfterBulkReset()`.
- `ColorKeyPair` (`dev.marie.framework.color`, marie-core) — a flat identity record pairing a background `ColorKey` with a text `ColorKey`, no logic. `MarieColors.registerColorPair(modId, id, backgroundDefaultArgb, textDefaultArgb)` builds the `panel.<id>`/`text.<id>` keys, registers both under the same registration-phase guard as `registerColor`, and returns the pair — so panels needing a background+text combo no longer hand-register two separate `ColorDefinition`s. `MarieColors` stays theme-free (no `ColorKeyPair`-specific resolve helper; call `resolveColor` per-key as before). `ColorPairRowGroup.buildRows(pair, backgroundLabel, textLabel)` (`dev.marie.framework.client.config.cloth`, marie-ui) builds the matching pair of `ColorHexRowWidget` rows for a Cloth Config category.

### Changed

- `ModuleRegistry` / `ComponentState` / `MarieComponent` (`dev.marie.framework.ui.component`, marie-ui) now carry `@ApiStatus.Experimental`, matching the tier already used on `DraggableResizable` / `ForeignScreenDetector` in the same module — these three were previously unannotated despite being load-bearing extension points with real external consumers. Documentation/API-surface clarity only, no behavior change.
- `RuntimeResolver` (`dev.marie.framework.runtime`, marie-core) now owns the shared `RecipeInheritanceResolver` instance directly instead of a dead, never-populated `recipeCache` field — `invalidateCache()` now actually clears the real recipe-inheritance index instead of a no-op field. Consumers (e.g. Nourished's `RuntimeFoodResolver`) now source the shared instance from `RuntimeResolver.getInstance()` instead of owning their own copy, so there is exactly one instance and one owner.

### Fixed

- `ValueEffectsListener.fireStateTicks` (marie-core) now requires `player.isInWater()` in addition to `player.isSwimming()` before firing the swim tick trigger. `isSwimming()` reflects vanilla's smoothed swim-pose flag and can stay `true` briefly after the player has actually left the water (e.g. exiting at speed while sprinting), which was causing swim triggers — and the swim HUD count — to keep firing on dry land.
- `SourceApplicationPipeline.process` (marie-core) now injects the player's `DiminishingReturnsConfig` into `TrackingData` (`tracking.setMemoryConfig(...)`) before posting `MarieEvents.SourceTriggerEvent`, not after. Consumer-mod listeners subscribed to `SourceTriggerEvent` that synchronously read tracking data (e.g. `toDeltaPayload()`, `getMostFatiguedFamilies()`, `config()`) previously ran against a `TrackingData` with a null `memoryConfig` and crashed with `IllegalStateException("[MarieLib] DiminishingReturnsConfig not injected...")`. Root cause was event/injection ordering, not a missing injection call — no other injection sites were touched.
- `GameplayTriggerListener` / `ValueEffectsListener.fireStateTicks` (marie-core) now pass `ItemStack.EMPTY` instead of `null` when firing `BLOCK_BROKEN` / `ENTITY_KILLED` / `TICK` triggers — the two-arg `MarieAPI.fireSourceTrigger` overload resolves to a `null` stack internally, which crashed downstream item-agnostic consumers (e.g. Nourished's `registerSlim` callback) with an NPE on `.getItem()` on every sprint/swim tick. `ItemStack.EMPTY` is the documented "no item" sentinel for non-item triggers per `ValueSourceTrigger`'s own convention.
- Removed leftover `TEMPDEBUG` diagnostic logging from `InstanceTagSourceRegistry.contains()` (marie-core) and `InstanceTagRegistry.contains()` (marie-resources) — both fired an ungated `LOGGER.info` on every call and had become log noise now that `CommunityTagResolutionStage` calls into this path far more frequently.
- `MarieNetworking.RATE_LIMIT_STATE` (marie-core) no longer grows unboundedly on a long-running server — `PlayerTrackingLifecycle.onPlayerLogout` now also calls new `MarieNetworking.clearRateLimitState(UUID)`, alongside its existing `TrackerManager.clearDirtySyncState` cleanup call.
- Closed the documented "no general reload happened, please re-register" gap for `TrackerRegistry`/`ColorDefinitionRegistry` (`MarieApiRegistries.onDatapackApplyBegin`'s known-gap doc): `ReloadGuardListener` now subscribes to NeoForge's `OnDatapackSyncEvent` and invokes `MarieContext.reloadBroadcastHook()` whenever `event.getPlayer() == null` (the full-reload case, as opposed to a single player's join-time sync). That event fires from `PlayerList.reloadResources()`, itself only called after `MinecraftServer.reloadResources()`'s returned future resolves — i.e. strictly after `MarieApiRegistries`' reset/refreeze of `TrackerRegistry`/`ColorDefinitionRegistry` for that reload pass has already completed — and it fires for both vanilla `/reload` and a mod's own reload command, whereas the existing `reloadAndBroadcast(server)` call was previously wired into only the latter (`MarieDatapackCommands.reloadAll`, now removed in favor of the automatic hook so it doesn't fire twice on that path). Reused the existing `MarieContext.reloadBroadcastHook()`/`onReloadBroadcast(...)` hook rather than adding a new one — it was already `@ApiStatus.Experimental` with no prior callers to break, and its "reload happened, notify" semantics already fit; only its javadoc needed clarifying, not its name. `TrackerRegistry.register`/`ColorDefinitionRegistry.register` now upsert on a duplicate key (new `AbstractRegistry.upsert`) instead of throwing `IllegalStateException`, since consumers are now expected to legitimately call them again from this hook on every reload; every other `AbstractRegistry` subclass keeps the original duplicate-throwing `register()`.
- `ReloadGuardListener.reloadAndBroadcast` was invoking `MarieContext.reloadBroadcastHook()` while `TrackerRegistry`/`ColorDefinitionRegistry` were already frozen (that freeze completes in `MarieApiRegistries.onDatapackApplyEnd`, strictly before this hook fires), so a consumer's hook calling `registerTracker`/`registerColor` — the intended re-registration pattern the hook exists for — threw `IllegalStateException("registry is frozen")` instead of upserting. New internal `AbstractRegistry.unfreeze()` (exposed as `TrackerRegistry.unfreezeInternal()`/`ColorDefinitionRegistry.unfreezeInternal()`) restores the frozen entries into the mutable map without clearing them; `reloadAndBroadcast` now calls it on both registries immediately before invoking the hook and refreezes both in a `finally` block immediately after, so the freeze guarantee holds everywhere except this one coordinated window and survives a throwing hook. No other registry is touched.
- Closed a second, more severe instance of the same "no general reload happened, please re-register" gap: `AddReloadListenerEvent` (which `MarieApiRegistries` resets `TrackerRegistry`/`ColorDefinitionRegistry` in response to, via `MarieDataLoader.apply`) fires on *every* world/server boot as well as on `/reload`, but `OnDatapackSyncEvent` — the event `reloadAndBroadcast` was wired to — only fires from `PlayerList.reloadResources()`, which is reachable exclusively from the explicit-`/reload` code path (`MinecraftServer.reloadResources(Collection)`), never from ordinary server startup (traced via NeoForge's vanilla patches: the boot-time resource load runs through `WorldLoader.load`, called from `Main.main` before a `MinecraftServer` instance even exists, and never touches `PlayerList.reloadResources()`). So every mod-init registration made via `MarieTracking.registerTracker`/`MarieColors.registerColor` was being wiped by the very first reload pass on every world load with nothing to restore it, and would only reappear after an explicit `/reload`. `ReloadGuardListener.onServerStarting` (`ServerStartingEvent`, already subscribed for `ReloadPipeline.reloadAll()`) now also calls `reloadAndBroadcast(event.getServer())` — the first point after boot with both a valid server reference and a guarantee the initial `AddReloadListenerEvent` reload pass has already completed. Since `MarieAPIState`'s `DatapackReloadScope` (opened for `MarieDataLoader.apply()`) already closed by the time `ServerStartingEvent` fires, `reloadAndBroadcast` now also explicitly reopens the registration window (`MarieAPIState.openForDatapackReload()`) around the hook call, rather than assuming it's still open — the same unfreeze/refreeze-in-`finally` safety it already applied to the two registries now also covers the phase gate.
- `GenericStateSyncPayload`'s size ceiling (`MarieNetworking.MAX_PAYLOAD_BYTES`) is now enforced by its `STREAM_CODEC` decoder from the raw wire byte count, before the `CompoundTag` is decoded, instead of `MarieNetworking.exceedsMaxSize` re-serializing the already-decoded tag to measure it after the fact. A malicious client could previously force a full NBT decode plus a second full re-serialization on the server thread for every oversized packet before it was rejected. The record gained a decode-time-only `oversized` field (never written to the wire, never true for a client-constructed payload) that `MarieNetworking.handleServer` now checks first, ahead of the unloaded-chunk/reach checks; externally-visible behavior (silent debug-logged drop, no disconnect) is unchanged.

## [MariesLib 0.1.1-beta.5] — 2026-07-26

Two new generic, consumer-agnostic primitives: client-side foreign-screen detection by menu-type registry name, and a generic block-scoped state sync packet from client to server.

### Added

- `ForeignScreenDetector` (`dev.marie.framework.ui`, marie-ui)
    - Lets a consuming mod register a `(ResourceLocation menuTypeId, Consumer<Screen> callback)` pair and get called back whenever a `ScreenEvent.Opening` screen's menu type matches, by registry name only
    - Never references any foreign mod's screen/menu class — matches purely via `BuiltInRegistries.MENU.getKey(...)` read off the opened menu
    - Lazily subscribes to `NeoForge.EVENT_BUS` on first `registerInterest` call
- `GenericStateSyncPayload` (`dev.marie.framework.network`, marie-core)
    - `CustomPacketPayload` carrying a `BlockPos` plus an opaque `CompoundTag`, for a consuming mod to sync small block-scoped state to the server without defining its own payload type or channel
    - `sendToServer(BlockPos, CompoundTag)` for client-side callers
- `MarieAPI.registerGenericStateSyncHandler(BiConsumer<ServerPlayer, GenericStateSyncPayload>)`
    - Registers a server-side handler for inbound `GenericStateSyncPayload`s, gated by `MarieAPIState.assertRegistrationAllowed` like the rest of `MarieAPI`'s registration surface
    - `MarieNetworking` registers the payload type via `RegisterPayloadHandlersEvent` and dispatches received payloads to all registered handlers

### Changed

- `MarieValueColors` / `GuiValueRenderer` (`dev.marie.framework.client.config.render`, marie-ui) now carry `@ApiStatus.Experimental`, matching their confirmed use by an external consumer mod — documentation/API-surface clarity only, no behavior change

### Fixed

- `ForeignScreenDetector.onScreenOpening` no longer crashes when the opened screen's menu wasn't constructed through the standard type-registry path (e.g. `advancements_reloaded`'s custom advancements screen), which previously threw an uncaught `UnsupportedOperationException` from `AbstractContainerMenu.getType()`
    - A screen with no menu is now skipped silently; a menu that rejects `getType()` is logged at DEBUG and treated as no match instead of propagating the exception

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
    - `writeReadmeIfAbsent` looks up `data/<modId>/config/...` using the _consuming_ mod's runtime `modId`, so a copy bundled under marie-ui's own `marieslib` namespace was never reachable by any real consumer, same bug class as `SOURCE_CLASSIFICATIONS_README`
    - Consumer mods must now bundle their own copy under their own `data/<modid>/config/` namespace
- Removed `COLORS_README.md` / `SCANNER_SPEC_README.md` from marie-core's bundled resources for the same reason
    - `ColorRegistry`/`ScannerSpecRegistry`'s `writeReadmeIfAbsent` resolve `data/<modId>/config/...` by the consuming mod's runtime `modId`, so the copies bundled under marie-core's own `marieslib` namespace were never reachable
    - Only affects the READMEs — `ScannerSpecRegistry`'s bundled `scanner_spec.json` _defaults_ use a separate, already-correct resolution path (`data/<modId>/<modId>/scanner/scanner_spec.json`, supplied by each consumer) and were not touched
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
