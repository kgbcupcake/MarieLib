# MariesLib — Architecture Reference

MariesLib is the shared engine behind Marie mods. It handles value tracking, source classification, compat discovery, registry lifecycle, and client sync plumbing. Consuming mods (like Nourished) wire domain-specific resolvers and UI on top.

For nutrition-specific terminology, see [Nourished ARCHITECTURE](https://github.com/kgbcupcake/nourished/blob/main/ARCHITECTURE.md).

---

## 1. Terminology

When in doubt, refer back here before introducing new names in code or datapacks.

---

### Value

A single tracked bar for a player, stored as a float between `0.0` and `1.0`. Values are registered through `ValueDefinition` and drive decay, thresholds, effects, and HUD rendering.

> A value is a bar. It goes up when you apply a source. It decays over time.

---

### Source

Anything that contributes to a value: an item (`ResourceLocation`), a custom trigger (`ValueSourceTrigger`), or a registered classification mapping. Sources are classified before application.

---

### Category

The dominant value key assigned when a source is applied — whichever value receives the largest share of the application. Category is used for the primary bar update and for `categoryMemory` fatigue tracking.

> Category is the label on the application. Value is the bar it feeds.

---

### Family

An optional string grouping for sources that are similar in kind (`"fish"`, `"bread"`, `"ore"`). Resolved by the consuming mod's `sourceFamilyResolver` hook. Used only for `familyMemory` fatigue — not for the primary bar assignment.

Family is nullable. Not every source needs one.

> Family answers: "have I been applying the same _kind_ of thing too much?"

---

### Classification

The process by which the pipeline inspects a source item and determines value scores, a dominant category, and an optional family. Classification is a pipeline stage, not a stored data structure.

Signals come from tags, namespaces, keywords, recipe inheritance, and custom `SourcePropertySignal` hooks. They aggregate into a score map.

> Classification is the verb. Category and family are the nouns it produces.

---

### The pipeline

```text
Source item or trigger
    │
    ▼
Classification (tags, scanner, runtime resolver)
    │
    ▼
Scores aggregated → dominant category + optional family
    │
    ▼
SourceApplicationPipeline.process(...)
    │
    ├─→ value map updated
    ├─→ sourceMemory updated
    ├─→ categoryMemory updated
    └─→ familyMemory updated
```

---

### Memory

The three server-side maps on `TrackingData`:

- **`sourceMemory`** — per source id
- **`categoryMemory`** — per dominant category key
- **`familyMemory`** — per family string

Each entry tracks a weighted apply count with a timestamp and decays over a configured window. Memory never leaves the server raw; the client may receive derived hints for display.

> Memory is what you applied lately. It drives diminishing returns, not the bars directly.

---

### Fatigue

Loss of application efficiency from repeating the same source, category, or family before memory fades. Decayed counts feed a diminishing curve blended into the final multiplier. High fatigue = lower gain per application, not zero gain.

---

### Debt

A soft variety nudge: when the dominant category you're applying has high category memory, the system may tick down a different value bar — the one with the lowest level. Not a currency. Just a gentle push toward rotation.

---

## 2. Runtime model

### What lives where

**Server only:**

- All value calculations (`SourceApplicationPipeline`)
- Tracking storage (`TrackingData` via `TrackingAttachment`)
- Memory maps and diminishing-return multipliers
- Effect application and threshold crossing
- Classification and scanner pipeline

**Client only:**

- `MarieClientCache` — read-only snapshot of the last sync packet
- HUD rendering (provided by consuming mod)
- Bar animations

**Both:**

- `TrackingData` exists on both sides, but the client copy is a display shell. Memory maps are server-side only and do not travel to the client.

---

### MarieClientCache

`TrackingData` on the server is the real state — values, aggregate total, memory, timestamps.

`MarieClientCache` holds whatever the server last sent: bar values and display hints. It does no math. It is replaced wholesale when a sync packet arrives.

They are separate for thread safety. The server writes on the server thread. The client reads on the render thread. The cache snapshot is `volatile` so the render thread always sees a complete old or new snapshot, never a half-updated mix.

---

### Lifecycle from login to logout

```text
Player logs in
    │
    ▼
TrackingData loaded from attachment (or created fresh)
Starting values set from config
    │
    ▼
Server sends full snapshot → MarieClientCache.set()
    │
    ▼
Player applies a source
    │
    ├─ SourceApplicationPipeline on server thread
    ├─ Classification resolved (override → scanner → defaults)
    ├─ Memory updated, multiplier computed
    ├─ Value deltas applied (modifiers, synergies)
    ├─ Threshold crossings checked → events fired
    ├─ Effects applied if enabled
    ├─ TrackingData saved to attachment
    └─ Delta packet sent → MarieClientCache.applyDelta()
    │
    ▼
Player logs out
    │
    └─ TrackingData saved via NeoForge attachment system
```

---

## 3. Override priority stack

Source classifications resolve through three layers, lowest to highest priority:

```text
bundled defaults  →  config override  →  datapack override
   (lowest)                                   (highest)
```

**Bundled defaults** — Shipped with the consuming mod so things work out of the box.

**Config override** — Server owner tuning via `config/<modid>/`. Changes behavior and classifications exposed in config.

**Datapack override** — Modpack maker control. Always wins for classification assignments.

> Specificity wins. Datapack requires the most intentional setup, so it sits on top.

---

## 4. Threading model

Three threads during normal gameplay:

**Server thread** — owns all tracking state. Pipeline, memory, multipliers, threshold checks, and effect application run here. Nothing else should write to `TrackingData`.

**Network thread** — carries packets. Client cache updates (`MarieClientCache.set()` / `applyDelta()`) happen here.

**Render thread** — reads display state only. HUD reads from `MarieClientCache`. Never writes. Never touches server `TrackingData` directly.

Memory maps never travel to the client.

> If it calculates, server thread. If it displays, render thread reading the cache. Network thread is the one-way bridge.

---

## 5. Extension points

Three ways to extend a Marie mod. Different tools for different jobs.

### Java API (`MarieAPI`)

For mods that depend on MariesLib in code. Register values, classifications, effects, compat, synergies, hooks. All registration during mod init.

### Datapacks

For modpack makers who want to reclassify sources or override values without Java. Datapacks sit at the top of the override stack.

### NeoForge events (`MarieEvents`)

For reacting to value changes without owning the source or the player. Subscribe to `ValueChangedEvent`, `SourceAppliedEvent`, etc. Use `ValueModifierEvent` to intercept deltas before they land.

---

## 6. Registry lifecycle

Keyed registries follow `reset → register → freeze`:

1. **`reset()`** — clears mutable storage, returns to registration phase (used on reload)
2. **`register()`** — appends entries while unfrozen; throws after `freeze()`
3. **`freeze()`** — copies into immutable snapshots for runtime reads

Addon registries: register during mod init before the server starts. Late registration throws.

### load() vs reload()

- **`load()`** — cold startup. Called once from mod construction. Creates default files, populates registries from bundled JSON.
- **`reload()`** — runtime refresh after config edits, import, or `/marie reload`. Typically `reset()`, re-parse, `freeze()`.

Datapack JSON uses a separate path via `RegistryLifecycleManager.loadAll(ResourceManager)` on `/reload`.

### RegistryLifecycleManager

Consuming mods register their config-backed registries in dependency order during construction. `RegistryLifecycleManager.loadAll()` runs at bootstrap; `reloadAll()` runs on server start and manual reload.

Nourished registers its nutrition-specific registries in `NourishedLifecycle` — see Nourished ARCHITECTURE for that list.

---

## 7. Multi-mod support

`MarieModRegistry` lets multiple mods register with MariesLib simultaneously. Each gets its own `MarieLibContext`, feature flags, command namespace, and config screen hooks.

`MariesLibBootstrap.attach(modId, bus)` is the minimal entry. `MarieLibContext.builder(modId)` is the full integration path.

---

## 8. What MariesLib is not

**Not a gameplay mod.** Install it because another Marie mod needs it. It does nothing visible on its own.

**Not domain-specific.** No nutrients, no food groups, no gut health. Those live in Nourished.

**Not a monolith.** Consuming mods own their UI, their classification stages, and their gameplay modules. The library owns the plumbing.

**Not opinionated about your modpack.** Sensible defaults ship with each consumer, but datapacks and config can override everything.
