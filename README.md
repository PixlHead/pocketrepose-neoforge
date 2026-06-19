# Pocket Repose (NeoForge clone)

A clean-room **NeoForge 1.21.1** reimplementation of the gameplay of the *Pocket Repose* mod, built
with **All The Mods 10 (ATM10)** in mind. Craft a key, name it, and step into your own private (or
shared) pocket dimension.

> **Status / honesty note:** This is a complete, hand-written source project. It was authored in an
> environment with **no network access**, so it has **not been compiled or run** — Gradle could not
> download NeoForge/Minecraft to build it here. Treat it as a strong starting point: it should build
> and run, but expect to fix the occasional small thing in your own dev environment. All the
> 1.21.1-specific API signatures were checked against NeoForge docs, but nothing beats an actual
> compile.

---

## What it does

The core loop mirrors the original mod:

1. **Craft a Keystone** (the "key") and a **Traveler's Suitcase**.
2. **Rename the Keystone in an anvil** — the name *is* the dimension. Two keys with the same name open
   the **same** pocket dimension (so names can be shared with friends, or kept secret as a password).
3. **Right-click the Keystone in the air** to *activate* it. This sanitizes the name and creates the
   pocket dimension if it doesn't already exist. Activated keys shimmer.
4. **Right-click a placed Suitcase with the activated Keystone** to *link* them. The suitcase lid opens.
5. **Right-click the linked Suitcase** (empty hand) to **enter**.
6. Inside, there's a small grass island with a tree and a glowing **Exit Portal**. Right-click the
   portal (or stand in it) to leave. You return to the suitcase you came from.

### Extra mechanics

- **No hostile spawns** inside pocket dimensions — they're safe spaces. (You can still bring mobs in.)
- **Move the suitcase, move the exit.** If someone breaks and replaces your linked suitcase (within a
  few blocks) while you're inside, you'll pop out at its new spot. Break it entirely and you exit
  where it used to be.
- **Set your entry point:** right-click the ground with a **bone** inside a pocket dimension to make
  that spot the place players appear when they enter.
- **Capture & release mobs:** right-click a mob with the Suitcase *item* to scoop it up (one at a
  time; bosses and players excluded), then right-click a block face to release it. Handy for moving a
  pet into your pocket.

### Commands

All require permission level 2 (op):

- `/pocketrepose allowRecursion <true|false>` — allow/deny opening a suitcase *from inside* a pocket
  dimension (nesting). Default: allowed.
- `/pocketrepose spawnIsland <true|false>` — whether *newly created* pocket dimensions get the
  decorative island (a safety platform + exit portal are always placed regardless).
- `/pocketrepose resetplayerentry <dimensionName>` — reset a dimension's entry point to the default.

### Recipes

- **Keystone:** two gold ingots stacked over one ender pearl (vertical).
- **Traveler's Suitcase:** a ring of eight leather around one ender pearl.

---

## Dependency: Infiniverse

Pocket dimensions are registered at runtime using **Commoble's Infiniverse**, a small, server-side,
vanilla-client-compatible library for dynamic dimensions. It is declared as a **required dependency**.

- **ATM10 may already include Infiniverse** (several popular dimension mods depend on it). If it's
  already in your pack, you don't need to add anything.
- If it isn't present, install Infiniverse for **NeoForge 1.21.1**:
  - CurseForge: search "Infiniverse" (Commoble) and grab the **1.21.1 / NeoForge** file
    (`infiniverse-1.21-2.0.1.0.jar`).

### How the build gets Infiniverse

Commoble's own maven only hosts Infiniverse for **1.21.9+**, so the build pulls the 1.21.1 build from
CurseForge via **CurseMaven** instead. This is configured in `build.gradle` / `gradle.properties`:

```
# gradle.properties
infiniverse_project_id=568341   # CurseForge project id for Infiniverse
infiniverse_file_id=5486311     # file id for infiniverse-1.21-2.0.1.0.jar (MC 1.21.1, NeoForge)
```

The resulting dependency is `curse.maven:infiniverse-568341:5486311`. If you ever need a different
build, the **file id** is just the number at the end of a file's CurseForge URL — open the file at
<https://www.curseforge.com/minecraft/mc-mods/infiniverse/files/all> and copy it.

If CurseMaven is ever unreachable, an equally valid alternative is to drop the jar into a `libs/`
folder and depend on it locally — replace the `implementation "curse.maven:..."` line in
`build.gradle` with:

```gradle
implementation files('libs/infiniverse-1.21-2.0.1.0.jar')
```

---

## Building

Requirements: **JDK 21**. (Gradle can fetch one automatically via the foojay toolchain resolver in
`settings.gradle`.)

```bash
# from the project root
./gradlew build      # Windows: gradlew build
```

The finished mod jar lands in `build/libs/`.

### About the Gradle wrapper

This download ships `gradle/wrapper/gradle-wrapper.properties` but **not** the binary
`gradle-wrapper.jar` (and the `gradlew` / `gradlew.bat` scripts may be missing), because those binary
pieces couldn't be generated offline. Pick whichever is easiest:

- **You already have Gradle 8.8+ installed:** run `gradle wrapper` once in the project root. It will
  generate `gradle-wrapper.jar`, `gradlew`, and `gradlew.bat`, after which `./gradlew build` works.
- **Open it in an IDE:** IntelliJ IDEA (with the Gradle plugin) will import the project and fetch
  everything for you. "Build Project" / the Gradle tool window then just works.
- **No Gradle at all:** install it (e.g. `sdkman`, `scoop install gradle`, or your package manager),
  then `gradle wrapper`.

### Matching your pack's NeoForge version

`neo_version` in `gradle.properties` defaults to a recent **21.1.x** build. For best results, set it
to the exact NeoForge version your ATM10 instance uses (any 21.1.x should be compatible). Versions:
<https://maven.neoforged.net/releases/net/neoforged/neoforge/>

---

## Installing into All The Mods 10

1. Build the jar (above), or grab it from `build/libs/`.
2. Drop it into your ATM10 instance's `mods/` folder.
3. Ensure **Infiniverse** for NeoForge 1.21.1 is also in `mods/` (it may already be there).
4. Launch. The dimension type and recipes load from the mod's bundled data pack automatically.

This is a **server-side-driven** feature set; vanilla-compatible clients can connect to a server
running it, but for single-player and to see the items/blocks, install it on the client too.

---

## Caveats & notes

- **Don't also run the original Fabric *Pocket Repose* via Sinytra Connector.** This clone uses the
  mod id `pocketrepose`, the same as the original. Loading both at once would cause a duplicate-mod
  ID crash. Use one or the other.
- **Placeholder textures.** The four PNG textures (suitcase side/top, exit portal, keystone) are
  simple hand-generated 16×16 placeholders. They're fully functional — replace them with nicer art
  any time at `src/main/resources/assets/pocketrepose/textures/...`.
- **Dimension persistence.** Infiniverse doesn't itself persist dynamically created dimensions across
  a server restart, so this mod records every created pocket dimension in saved data (attached to the
  overworld) and re-creates them on `ServerStarted`. The chunk data on disk is preserved, so your
  builds inside a pocket survive restarts.
- **Config vs. commands.** `META-INF`/config defaults live in the server config
  (`allowRecursion`, `spawnIsland`, `entryHeight`); the commands above change the live values, which
  are stored in saved data so they persist.

---

## Project layout

```
src/main/java/com/pocketrepose/
  PocketRepose.java            # @Mod entry point; registers everything
  Config.java                  # server config (recursion, island, entry height)
  registry/                    # blocks, items, block entities, data components,
                               #   chunk generator, dimensions, creative tab
  block/                       # SuitcaseBlock (+ BlockEntity), ExitPortalBlock
  item/                        # KeystoneItem, SuitcaseItem
  world/                       # PocketChunkGenerator (void), PocketDimensionManager,
                               #   PocketReposeState (SavedData)
  util/                        # TeleportHelper (enter/exit, cooldowns)
  event/                       # ModEvents (commands, spawn suppression, bone entry, startup)
src/main/resources/
  assets/pocketrepose/         # blockstates, models, lang, textures
  data/pocketrepose/           # dimension_type, recipes
  data/minecraft/tags/         # mineable/axe entry for the suitcase
  META-INF/neoforge.mods.toml  # metadata + dependencies
```

## License

MIT — see `LICENSE`. Clean-room reimplementation; contains no code or assets from the original mod.
