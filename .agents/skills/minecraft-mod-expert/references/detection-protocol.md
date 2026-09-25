# Detection Protocol Details

Scanning rules, globs, and exact regex patterns for project identification.

## File Detection Globs & Regex

### 1. `gradle.properties`
- **Path**: `gradle.properties`
- **Regex Patterns**:
  - Minecraft Version: `^minecraft_version\s*=\s*(.+)`
  - NeoForge Version: `^(?:neo_version|neoforge_version)\s*=\s*(.+)`
  - Fabric Loader Version: `^loader_version\s*=\s*(.+)`
  - Fabric API Version: `^fabric_version\s*=\s*(.+)`
  - Yarn Mappings: `^yarn_mappings\s*=\s*(.+)`

### 2. `build.gradle` / `build.gradle.kts`
- **Paths**: `build.gradle`, `build.gradle.kts`
- **Regex Patterns**:
  - NeoForge ModDevGradle: `id\(?['"]net\.neoforged\.moddev['"]\)?`
  - Fabric Loom: `id\(?['"]fabric-loom['"]\)?`
  - Java Toolchain: `languageVersion\s*=\s*JavaLanguageVersion\.of\((\d+)\)`

### 3. `gradle-wrapper.properties`
- **Path**: `gradle/wrapper/gradle-wrapper.properties`
- **Regex Pattern**:
  - Gradle Version: `distributionUrl=.*gradle-(\d+\.\d+(?:\.\d+)?)-(?:bin|all)\.zip`

### 4. Mod Metadata Files
- **NeoForge**: `src/main/resources/META-INF/neoforge.mods.toml` or `mods.toml`
  - Mod ID Regex: `^modId\s*=\s*['"]([^'"]+)['"]`
- **Fabric**: `src/main/resources/fabric.mod.json`
  - Mod ID JSON key: `"id"`

## Output Schema
Always output exact JSON format defined in `SKILL.md`.
Mark non-matching fields as `"UNKNOWN"`.
