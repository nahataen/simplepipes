#!/usr/bin/env bash
# Detection script for Minecraft Java mod projects

LOADER="UNKNOWN"
MC_VER="UNKNOWN"
LOADER_VER="UNKNOWN"
JAVA_VER="UNKNOWN"
GRADLE_VER="UNKNOWN"
MAPPINGS="UNKNOWN"
PLUGIN="UNKNOWN"

if [ -f "gradle.properties" ]; then
    MC_VER=$(grep -E "^minecraft_version\s*=" gradle.properties | cut -d'=' -f2 | tr -d ' ' || echo "UNKNOWN")
    NEO_VER=$(grep -E "^(?:neo_version|neoforge_version)\s*=" gradle.properties | cut -d'=' -f2 | tr -d ' ' || echo "")
    FABRIC_VER=$(grep -E "^fabric_version\s*=" gradle.properties | cut -d'=' -f2 | tr -d ' ' || echo "")
    
    if [ -n "$NEO_VER" ]; then
        LOADER="NeoForge"
        LOADER_VER="$NEO_VER"
    elif [ -n "$FABRIC_VER" ]; then
        LOADER="Fabric"
        LOADER_VER="$FABRIC_VER"
    fi
fi

if [ -f "src/main/resources/META-INF/neoforge.mods.toml" ] || [ -f "src/main/resources/META-INF/mods.toml" ]; then
    [ "$LOADER" = "UNKNOWN" ] && LOADER="NeoForge"
elif [ -f "src/main/resources/fabric.mod.json" ]; then
    [ "$LOADER" = "UNKNOWN" ] && LOADER="Fabric"
fi

if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    if grep -q "net.neoforged.moddev" build.gradle* 2>/dev/null; then
        PLUGIN="ModDevGradle"
    elif grep -q "fabric-loom" build.gradle* 2>/dev/null; then
        PLUGIN="Loom"
    fi
fi

if [ -f "gradle/wrapper/gradle-wrapper.properties" ]; then
    GRADLE_VER=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | sed -n 's/.*gradle-\([0-9.]*\)-.*/\1/p' || echo "UNKNOWN")
fi

cat <<EOF
{
  "loader": "${LOADER:-UNKNOWN}",
  "minecraft_version": "${MC_VER:-UNKNOWN}",
  "loader_version": "${LOADER_VER:-UNKNOWN}",
  "java_version": "${JAVA_VER:-UNKNOWN}",
  "gradle_version": "${GRADLE_VER:-UNKNOWN}",
  "mappings": "${MAPPINGS:-UNKNOWN}",
  "build_plugin": "${PLUGIN:-UNKNOWN}"
}
EOF
