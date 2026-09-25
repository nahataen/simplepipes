# 🔧 SimplePipes

Mod para **Minecraft (Fabric)** que agrega **tubos para mover objetos automáticamente** entre cofres, hornos y otras máquinas, sin tener que cargarlos a mano.

## ✨ Qué agrega

- **Tubo** — conecta dos puntos y pasa objetos de uno a otro.
- **Tubo con filtro** — solo deja pasar los objetos que tú elijas.
- **Tubo de extracción** — saca objetos de un contenedor aunque no lo estés mirando.
- **Cofre explorador** — cofre especial con su propia pantalla para revisar lo que guardas.

Todo aparece en la pestaña de **Redstone** y **Bloques funcionales** del modo creativo.

## 📋 Qué necesitas para jugar

- Minecraft **26.1.2**
- **Fabric Loader 0.19.3** (o más nuevo)
- **Fabric API** instalada como mod

## 📥 Cómo instalarlo

1. Instala Fabric Loader para tu Minecraft (desde la página oficial de Fabric).
2. Pon el archivo de **Fabric API** y el `.jar` de **SimplePipes** dentro de la carpeta `mods` de tu Minecraft.
3. Abre el juego y listo: busca "tubo" en el creativo.

## 🎮 Cómo se usa

1. Coloca un cofre (o máquina) de origen y uno de destino.
2. Une ambos con tubos (se conectan solos al colocarlos juntos).
3. Pon un **tubo de extracción** pegado al origen para que empiece a mover cosas.
4. Si quieres filtrar, usa el **tubo con filtro** y elige qué objetos deja pasar.

## 🛠️ Para desarrolladores

```bash
gradlew.bat runClient   # prueba el mod en un Minecraft de desarrollo (Windows)
gradlew.bat build       # genera el .jar en build/libs
```

- Requiere **Java 21**.
- En Windows también puedes dar doble clic a `RunClient.bat`.

## 📄 Licencia

MIT — úsalo, modifícalo y compártelo libremente.
