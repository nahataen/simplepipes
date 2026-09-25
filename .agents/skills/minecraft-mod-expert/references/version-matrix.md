# Minecraft Modding Compatibility Matrix

Live metadata source URLs:
- NeoForge: `https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml`
- Fabric API: `https://meta.fabricmc.net/v2/versions`
- Gradle: `https://services.gradle.org/versions/all`

## Compatibility Table

| MC Version | Loader | Loader Version Range | Min JDK | Recommended Gradle | Verification Date |
|------------|--------|----------------------|---------|-------------------|-------------------|
| 1.21.1     | NeoForge | 21.1.0+             | 21      | 8.8+              | 2026-07-29        |
| 1.21.0     | NeoForge | 21.0.0+             | 21      | 8.8+              | 2026-07-29        |
| 1.20.6     | NeoForge | 20.6.0+             | 21      | 8.7+              | 2026-07-29        |
| 1.20.4     | NeoForge | 20.4.0+             | 17      | 8.5+              | 2026-07-29        |
| 1.21.1     | Fabric   | Loader 0.16.0+ / API 0.102.0+ | 21 | 8.8+        | 2026-07-29        |
| 1.20.4     | Fabric   | Loader 0.15.0+ / API 0.96.0+  | 17 | 8.5+        | 2026-07-29        |
| 1.20.1     | Forge / Fabric | Forge 47.2.0+ / Fabric 0.14.22+ | 17 | 8.1.1+  | 2026-07-29        |
| 1.16.5     | Forge / Fabric | Forge 36.2.39+ / Fabric 0.11.3+ | 8 / 11 | 7.6+   | 2026-07-29        |

## Version Rules
- **NeoForge Version Schema**: `XX.Y.Z` where `XX.Y` corresponds to Minecraft `1.XX.Y` (e.g. NeoForge `21.1.5` -> MC `1.21.1`).
- **JDK Requirements**:
  - MC 1.20.5+ -> Java 21
  - MC 1.18 - 1.20.4 -> Java 17
  - MC 1.17 -> Java 16
  - MC 1.16.5 -> Java 8 or Java 11
