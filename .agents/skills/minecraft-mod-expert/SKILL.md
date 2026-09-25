---
name: minecraft-mod-expert
description: Expert Minecraft Java modding assistant for NeoForge and Fabric across Gradle, mappings, and Java toolchains. Use when building, debugging, refactoring, or extending Minecraft Java mods, or configuring Gradle build files for NeoForge or Fabric.
---

# Minecraft Mod Expert

## Overview
Protocol-driven guidance for Minecraft Java modding across NeoForge and Fabric loaders. Ensures exact version detection, paradigm alignment, and mandatory documentation verification before code modification.

## Detection Protocol
Always scan build files and report JSON status before proposing or writing code.

### Required Scan Targets
- `gradle.properties`: `minecraft_version`, `neo_version`, `loader_version`, `fabric_version`, `yarn_mappings`
- `build.gradle` / `build.gradle.kts`: plugins (`net.neoforged.moddev`, `fabric-loom`), Java toolchain target
- `gradle-wrapper.properties`: Gradle distribution version
- Mod metadata: `META-INF/neoforge.mods.toml`, `META-INF/mods.toml`, or `fabric.mod.json`

### JSON Report Format
```json
{
  "loader": "NeoForge | Fabric | UNKNOWN",
  "minecraft_version": "STRING | UNKNOWN",
  "loader_version": "STRING | UNKNOWN",
  "java_version": "INTEGER | UNKNOWN",
  "gradle_version": "STRING | UNKNOWN",
  "mappings": "Mojang | Parchment | Yarn | UNKNOWN",
  "build_plugin": "ModDevGradle | Loom | UNKNOWN"
}
```
Rule: Mark missing fields as `"UNKNOWN"` and prompt user. Never assume.

## Structure Analysis Protocol
Inspect and list existing project assets before writing code:
- Java packages under `src/main/java`
- Assets under `src/main/resources/assets/<modid>/`
- Data assets under `src/main/resources/data/<modid>/` (recipes, loot_tables, tags)
- Data generators (`DataGenerator` classes)
- Mixin configuration files (`neoforge.mixins.json` / `fabric.mixins.json`)

## Documentation Citation Protocol
Every proposed change must cite official documentation for the detected version.

### Citation Format
`[Target File] | [Proposed Change] | [Doc Source + URL + Version]`

### Official Sources
- NeoForge: `https://docs.neoforged.net/`
- Fabric: `https://docs.fabricmc.net/`
- Gradle: `https://docs.gradle.org/<version>/`

## Loader Paradigm Rules
- **NeoForge 1.21+**: `DeferredRegister` instances registered to mod `IEventBus` inside mod constructor.
- **Fabric**: Entrypoints defined in `fabric.mod.json`, registration via `Registry.register` or Fabric API helpers.
- Never mix loader paradigms or APIs.

## Red Flags (Hard Restrictions)
- NEVER assume version if marked `UNKNOWN` — stop and ask user.
- NEVER use legacy Forge snippets (1.12–1.20.4) on NeoForge 1.21+.
- NEVER edit Java, JSON, or Gradle files without completing initial JSON detection report.
- NEVER force JDK upgrades unless project toolchain configuration demands it.
- NEVER quote unverified version numbers outside `references/version-matrix.md`.

## Verification Protocol
Before declaring task complete:
1. Run `./gradlew build` or `./gradlew check`.
2. Confirm no compilation errors or missing mapping symbols.
3. Validate JSON metadata syntax (`neoforge.mods.toml` / `fabric.mod.json`).
