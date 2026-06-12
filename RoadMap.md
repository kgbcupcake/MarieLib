# MariesLib Roadmap

MariesLib started as extracted plumbing from Nourished — registries, scanner, tracking, compat — and is now the shared backbone for Marie mods.

This roadmap covers framework and infrastructure work. Nutrition gameplay items live in [Nourished RoadMap](https://github.com/kgbcupcake/nourished/blob/main/RoadMap.md).

---

## Infrastructure

### Server-authoritative synchronization

- Registry synchronization across clients
- Tracking snapshot validation
- Multiplayer reliability for value deltas

### Validation engine

- Datapack schema validation
- Value key validation
- Classification coverage analysis
- Validation report generation

### Crash diagnostics

- Runtime state capture on failure
- Classification trace reporting
- Sync failure diagnostics

### Marie compiler (working name)

- Architecture validation
- Registry validation
- Configuration validation
- Compiler-style diagnostics with fix suggestions

---

## Datapack loaders

Schemas exist. Loaders still in progress for:

- `values/`
- `effects/`
- `synergies/`
- `source_synergies/`
- `milestones/`
- `tracking_profiles/`

Working now: `source_classifications/`, `compat/`, `source_families/`, `module_locks/`.

---

## Integrations

- KubeJS scripting expansion
- Broader JEI / REI / EMI tooltip coverage
- Third-party mod discovery improvements
- More diagnostic commands under `/marieslib`

---

## Long-term direction

MariesLib should be the layer other Marie mods (and eventually third-party mods) can depend on for:

- Source classification without hand-written heuristics
- Player tracking without custom save/sync/decay code
- Compat without hardcoded mod IDs
- Datapack tooling with validation
- Multiplayer-safe synchronization
