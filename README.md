# MariesLib

A shared infrastructure library for Marie's Minecraft mods. Ships embedded via JarJar — no separate download required.

---

## What is this?

MariesLib is the backbone powering all Marie mods. It provides the shared infrastructure that would otherwise be duplicated across every mod: the registry lifecycle system, classification pipeline, compat discovery framework, caching, and utilities.It is designed to be lightweight and easy to use. It is not a feature-rich mod, but a foundation for building mods that need to reason about items, classify content at runtime, or integrate cleanly with other mods.

If you're a player, you don't need to think about this. It ships inside each mod that uses it.

If you're a mod developer, MariesLib gives you a production-ready foundation for building mods that need to reason about items, classify content at runtime, or integrate cleanly with other mods. It is designed to be easy to use and understand, with a focus on simplicity and readability.

---

## What's inside

| Package    | What it provides                                                                                                                                                           |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `cache`    | `BoundedLRU`, `RunningAverage`: lightweight caching utilities                                                                                                              |
| `registry` | `AbstractRegistry`, `RegistryLifecycleManager`, `RegistrySnapshot`, `ListRegistry`: lifecycle-aware registry infrastructure                                                |
| `compat`   | `CompatRegistry`, `CompatEntry`, `JsonCompatEntry`, `CompatDefinition`, `ConflictBehavior`, `ConflictLevel`, `SemVer`: mod compatibility discovery and conflict resolution |
| `scan`     | Runtime classification pipeline _(in progress)_                                                                                                                            |
| `util`     | `MarieJsonUtils`, `MarieValidation`: JSON and validation helpers                                                                                                           |
| `network`  | Network sync infrastructure _(planned)_                                                                                                                                    |
| `config`   | Config snapshot and reload pattern _(planned)_                                                                                                                             |

---

## Mods built on MariesLib

| Mod                                             | Description                                                     |
| ----------------------------------------------- | --------------------------------------------------------------- |
| [Nourished](https://modrinth.com/mod/nourished) | Nutrition framework for NeoForge 1.21.1                         |
| MariePerfTools                                  | Block entity culling and performance tooling _(in development)_ |

---

## For mod developers

MariesLib ships embedded in each consuming mod via NeoForge JarJar. You do not publish it as a standalone dependency — include it in your own mod's jar.

Add to your `settings.gradle`:

```groovy
includeBuild('../MariesLib')
```

Add to your `build.gradle` dependencies:

```groovy
compileOnly "dev.marie.MariesLib:marieslib:${marie_lib_version}"
jarJar(implementation("dev.marie.MariesLib:marieslib:${marie_lib_version}")) {
    version {
        strictly "[${marie_lib_version},)"
        prefer marie_lib_version
    }
}
additionalRuntimeClasspath "dev.marie.MariesLib:marieslib:${marie_lib_version}"
```

Add to your `gradle.properties`:

```
marie_lib_version=1.0.0
```

---

## Migration notes

### CompatDefinition (pre-1.0.0)

- This is a breaking change.
  `CompatDefinition` previously lived at `dev.maire.nourished.api.CompatDefinition`.
  It has moved to `dev.marie.MariesLib.compat.CompatDefinition`.

If you are an addon developer depending on Nourished's API, update your import:

```java
// Before
import dev.maire.nourished.api.CompatDefinition;

// After
import dev.marie.MariesLib.compat.CompatDefinition;
```

This break occurred during the beta period. Future breaking changes will be accompanied by a deprecation shim and changelog notice.

---

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21

---

## License

[LGPL-3.0-only](LICENSE) — you may depend on MariesLib in any mod regardless of your mod's license. Modifications to MariesLib itself must be published under LGPL.

---

## Links

- [Nourished on Modrinth](https://modrinth.com/mod/nourished)
- [GitHub](https://github.com/kgbcupcake)
