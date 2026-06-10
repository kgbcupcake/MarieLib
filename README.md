![MariesLib Banner](Assets/MariesLib_Banner.png)

I kept rebuilding the same plumbing in every Marie mod — registries, compat discovery, source
classification, caching, JSON helpers. It worked, but it was duplicated everywhere and painful
to maintain, so I pulled it out into one library.

**MariesLib** is the shared backbone behind mods like [Nourished](https://modrinth.com/mod/nourished).
It is not a gameplay mod. It is the foundation other Marie mods build on.

---

## Community

Discord: [https://discord.gg/EZnFJsfQup]

Questions, suggestions, and development discussion are welcome.

---

## Do you need to install this?

**Yes — if you use a Marie mod that depends on it.**

Every Marie mod requires MariesLib as a **separate install**. [Nourished](https://modrinth.com/mod/nourished),
for example, needs **MariesLib 1.0.0+** alongside it.

Install both:

- The Marie mod you want (e.g. Nourished)
- MariesLib 1.0.0 or newer

Most launchers resolve that dependency automatically. If a Marie mod fails to load, check that
MariesLib is installed and up to date.

MariesLib is infrastructure, not a gameplay mod on its own. It powers the mods that depend on it.

---

## What MariesLib provides

MariesLib handles the shared infrastructure so Marie mods can focus on gameplay:

| System              | What it does                                                              |
| ------------------- | ------------------------------------------------------------------------- |
| Classification      | Runtime source resolution, scanner tooling, and classification traces     |
| Compat discovery    | Three-tier compatibility registry with modpack overrides                  |
| Registry lifecycle  | Lifecycle-aware registries with snapshots and reload support                |
| Tracking & values     | Player value tracking, memory windows, decay, and effect hooks            |
| Diagnostics         | Datapack validation, unknown-item logging, and debug commands               |
| Config tooling      | Presets, import/export, share codes, and module locks                       |
| Utilities           | JSON helpers, bounded LRU cache, running averages, validation             |

It is designed to stay lightweight and readable — a foundation, not a feature dump.

---

## The classification pipeline

This is the core of MariesLib.

When a consuming mod needs to reason about a source it has never seen before, MariesLib resolves
it through staged runtime logic:

- **Runtime resolution** with caching and cascade fallbacks
- **Token normalization** for domain-specific source vocabulary
- **Recipe inheritance** and multi-value blending where configured
- **Override registries** via `config/<modid>/source_overrides.json`
- **Classification traces** so developers can inspect why a source resolved the way it did

There is also a **developer-facing scanner** for bulk analysis of unclassified sources. In current
Marie mods, that means items with `FoodProperties` — such as Nourished's food classification
workflow. It is not a player-facing gameplay feature.

---

## Modularity

Marie mods built on MariesLib can toggle major features independently — source application,
decay, effects, HUD, toasts, and more. Modpack authors can lock modules server-side through
datapack module locks.

---

## 🔧 Configurable to your server

Everything ships with sensible defaults. Consuming mods expose the rest:

- Toggle individual modules on or off
- Adjust decay rates and thresholds per value
- Override sources via `config/<modid>/source_overrides.json`
- Override compat via `config/<modid>/compat_overrides.json`
- Drive definitions through datapacks where loaders are available
- Save and share full config snapshots with a single share code

---

## 🤝 Broad Mod Compatibility

MariesLib uses a **three-tier compat system** so mod authors, addon authors, and modpack creators
can declare and override compatibility without recompiling:

| Tier | Source                                                          | Notes                      |
| ---- | --------------------------------------------------------------- | -------------------------- |
| 1    | `data/<modid>/compat/compat_registry.json` in the consuming mod | Base registry              |
| 2    | `data/<other_modid>/marie_compat.json` from loaded mods         | Mod-provided declarations  |
| 3    | `config/<modid>/compat_overrides.json`                          | Modpack overrides          |

Later tiers merge into earlier entries rather than replacing them wholesale.

| Integration            | Status                        |
| ---------------------- | ----------------------------- |
| KubeJS                 | ✅ Scripting support           |
| Cloth Config           | ✅ Preset and import/export UI |
| JEI / REI / EMI        | ✅ Tooltips in recipe viewers  |
| Any Marie mod          | ✅ Required separate install   |

---

## Mods built on MariesLib

All Marie mods require MariesLib as a separate install.

| Mod                                             | Description                                     |
| ----------------------------------------------- | ----------------------------------------------- |
| [Nourished](https://modrinth.com/mod/nourished) | Nutrition framework for NeoForge 1.21.1         |
| MariePerfTools                                  | Block entity culling and performance tooling _(in development)_ |

---

## 🌐 For mod developers

MariesLib exposes a stable public API if you want to integrate with it:

```java
float level = MarieAPI.getValueLevel(player, "proteins");
MarieAPI.registerValue(definition);
MarieAPI.registerCompatEntry(definition);
MarieAPI.registerCustomEffect(thresholdEffect);
```

API elements are marked `@Stable`, `@Experimental`, or `@Internal` so you know exactly what
you can rely on.

Every Marie mod requires MariesLib as a **separate mod** on the classpath. There is no JarJar
bundling. Declare `marieslib` as a required dependency and wire your runtime through
`MarieLibContext` at bootstrap.

Addons can register custom values, source classifications, effects, compat entries, and events
through Java or KubeJS. Consuming mods can also ship datapack-only integrations without writing
Java code.

`CompatDefinition` previously lived at `dev.maire.nourished.api.CompatDefinition` and has moved
to `dev.marie.MariesLib.compat.CompatDefinition`.

---

## 📦 Datapack Support

Consuming mods can drive MariesLib through datapacks with zero Java code where loaders are available:

**Working now:**

- Source classification — assign items to value keys under `data/<namespace>/<modid>/source_classifications/`
- Compat entries — declare mod compatibility under `data/<namespace>/<modid>/compat/`
- Source families — group related sources under `data/<namespace>/<modid>/source_families/`
- Module locks — lock features server-side under `data/<namespace>/<modid>/module_locks/`

**Schema defined, loaders still in progress:**

- `values/`, `effects/`, `synergies/`, `source_synergies/`, `milestones/`, `tracking_profiles/`

The built-in scanner can auto-classify unknown sources and write tag recommendations and reports
for high-confidence hits.

---

## 🟨 KubeJS Support

KubeJS scripting support for value registration, source classifications, synergies, milestones,
and event hooks — no Java required.

```js
MarieAPI.registerValue({
    id: 'custom_value',
    displayName: 'Custom Value',
    decayRate: 0.02
})

MarieEvents.onValueChanged(event => {
    if (event.valueKey === 'custom_value' && event.level < 0.25) {
        event.player.tell('Your custom value is low!')
    }
})
```

---

## 🚧 Current Focus

- Completing remaining datapack loaders
- Expanding network sync infrastructure
- More diagnostic and validation tooling
- Broader third-party integration support

---

## Requirements

| | |
| --- | --- |
| **Minecraft** | **1.21.1** |
| **NeoForge** | **21.1.x** |
| **Java** | **21** |

---

## License

LGPL-3.0-only

---

## Links

- [Modrinth](https://modrinth.com/mod/marieslib)
- [Nourished on Modrinth](https://modrinth.com/mod/nourished)
- [GitHub](https://github.com/kgbcupcake)
