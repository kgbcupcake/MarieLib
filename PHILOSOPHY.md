# MariesLib — API Philosophy

## What MariesLib Is

MariesLib is a value-tracking and source-classification engine for NeoForge 1.21.1. It is infrastructure that Marie mods build on. The default experience in any given mod (HUD, effects, decay curves) is one implementation of that engine, not the engine itself.

Nourished is the reference consumer. If you only want nutrition integration, depend on Nourished, not this library directly.

## What We Guarantee

Everything annotated `@ApiStatus.Stable` in `dev.marie.MariesLib.api` is a public contract. We will not remove or change stable method signatures without a major version bump and a deprecation cycle.

## What We Don't Guarantee

`@ApiStatus.Experimental` may change between minor versions. KubeJS bindings are experimental.

`@ApiStatus.Internal` is not part of the public contract. It may change or disappear without notice. Do not depend on internal classes.

## What Addons Should Build Against

Only `dev.marie.MariesLib.api.*` and public types under `dev.marie.MariesLib.compat` where marked stable.

Do not import from `core`, `handler`, `tracking`, `scanner`, `classification`, `client`, `config`, `kubejs/internal`, or `api/impl`. Those packages are implementation details.

## What Addons Should Avoid

Do not hardcode specific value keys like `"proteins"` unless you control that consumer mod. Query registered keys at runtime.

Do not depend on specific internal balancing values. Those evolve with gameplay in each consumer.

Do not depend on HUD layout or rendering internals in consuming mods.

Do not reflect into internal classes.

## Configuration Layering

Four layers, lowest to highest priority: Java defaults → TOML config → config JSON files → datapack JSON.

Datapacks win. Prefer datapack JSON for source classifications and compat entries over Java registration where possible. Modpack authors get override authority.

## Data vs Code

Register values, effects, compat entries, and source classifications via datapack JSON when you can. Java API calls are for runtime-dynamic behavior. Static definitions belong in data.

## Versioning

Semantic versioning. Major bumps may break `@Stable` APIs with a migration guide. Minor bumps may evolve `@Experimental` APIs. Patch releases are stable.

Check `MarieAPIVersion.isCompatible(requiredMajor)` at startup if your addon requires a minimum API version.

## Ecosystem Intent

Marie mods are meant to share one tracking and classification layer, not each rebuild their own. If your mod adds items that should contribute to a value system, register them. If your mod affects player state, listen to `MarieEvents`. The goal is a coherent mod family, not a walled garden.

## Separation Is Done

The engine (tracking, APIs, registries, scanner) lives in MariesLib. Gameplay (HUD, domain-specific effects, nutrition modules) lives in consuming mods. That split is shipped, not planned.

Addons that only need the engine do not pull in Nourished's client rendering. Addons that need nutrition depend on Nourished.

## Contact and Contributions

Repository: [https://github.com/kgbcupcake](https://github.com/kgbcupcake)

Compat PRs and datapack contributions for consuming mods are welcome. For MariesLib API promotion requests (moving something from Internal to Stable), open an issue with your use case.
