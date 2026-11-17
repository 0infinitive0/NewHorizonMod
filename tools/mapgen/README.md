# Attack map generator

Writes three attack maps built around the nukes and what came with them into `assets/maps`, using the game's own code, so the
files are exactly what the game would save.

| Map | Size | What the enemy has |
| --- | --- | --- |
| Nuclear Standoff | 240 x 150 | Three Nuke Launchers, a Multi Nuke Launcher and an Orbital Launcher, each fed by its own Nuke Builder (explosive, incendiary, quake, swarm, radiation); a Cloak Projector, Nuke Jammer, two Nuke Interceptors, a Long-Range Radar and a Decoy Beacon |
| Leviathan Strait | 220 x 220 | A Serpent Nest and a Crab Dock at work, an Orbital Launcher (quake) and two Nuke Launchers (swarm, incendiary); only flying waves |
| Event Horizon | 200 x 200 | A Gravity Dock, a Modular Foundry, omni walls and two Omni Shields, launchers for scatter, EMP and cluster nukes; Chimeras in the waves; omni-matter deposits for the player |

The enemy team cheats (it needs no items and no power), so its builders, docks and launchers run by themselves. Its unit build speed
(`rules(...)` in `AttackMaps.java`) is what paces its nukes and its big units; the layouts and wave lists are in the same file.

## Running it

It needs a JDK 17 or newer, the mod's jar and the game's headless server jar (`server-release.jar` of the build in `build.gradle`,
from the Mindustry releases page).

```bash
./gradlew jar
```

```bash
mkdir -p build/mapgen/data/mods build/mapgen/classes
```

```bash
cp build/libs/NewHorizonModDesktop.jar build/mapgen/data/mods/
```

```bash
javac -cp server-release.jar:build/libs/NewHorizonModDesktop.jar -d build/mapgen/classes tools/mapgen/*.java
```

```bash
java -cp server-release.jar:build/mapgen/classes AttackMaps build/mapgen/data assets/maps
```

On Windows use `;` in place of `:` in the two class paths. It prints one line for each map, and a line starting with `!` for any
structure that did not fit where the layout puts it.

## Testing them

`MapTest` loads maps the way a custom game does and plays them with nobody at the controls, printing every game minute the wave, the
nukes launched, the player's core health and what the enemy has out. `-Dhold=true` keeps the player's core alive so a long run shows the
late units (the Serpent after about 25 minutes, the King Crab after about 45, the Gravity Base after about 50).

```bash
java -Dhold=true -cp server-release.jar:build/mapgen/classes MapTest build/mapgen/data assets/maps 60 build/mapgen nuclear-standoff leviathan-strait event-horizon
```

With nobody playing the core falls after roughly 10 to 17 minutes (the radar and two Interceptors it starts with are not enough); a
player who builds more defences gets much longer.
