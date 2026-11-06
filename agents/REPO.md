# Repo rules - ability-tensura

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in [`neoforge-mod-template`](https://github.com/HexaHexes/neoforge-mod-template)
and pulled in verbatim - **never hand-edit it.** Everything in this file is this
repo's own and is edited freely; keep it aligned with the org rules rather than
restating them.

**Repo:** `HexaHexes/ability-tensura` - **Mod ID:** `ability_tensura` - **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 - **Jar:** `build/libs/ability_tensura-<version>.jar`

Battlewill martial abilities for the Tensura RPG pack: weapon-style definitions
(sword, spear/polearm, axe/heavy, ranged, shield, unarmed), active techniques,
weapon enhancements, passives, resource conversion and costs, progression and
restrictions - datapack-driven for metadata, costs, restrictions and mappings,
with code adapters only where custom execution behaviour demands it.

**Dependencies**
- Required: Minecraft, NeoForge, `tensura-ability-core` (from GitHub Packages
  via `PACKAGE_READ_TOKEN`, scoped to the Gradle step only).
- Optional: `ability_hotbar`, `tensura-better-spellcasting-bridge`,
  `better_spellcasting`, `irons_spellbooks` - never hard dependencies; all
  integration through soft hooks.

**Release plan:** [docs/release-plan.md](https://github.com/HexaHexes/ability-core/blob/main/docs/release-plan.md) - the release train, per-repo checklist, core-before-consumers sequencing and ALPHA_RELEASES state (hub: ability-core).

**Test:** `./gradlew runGameTestServer` - `AbilityTensuraGameTests` (1 test). `expectedTests = 1`
in `build.gradle` must match the `@GameTest` count.

**Gotchas**
- **Data-driven does not mean arbitrary behaviour.** Datapacks carry metadata,
  costs, restrictions and mappings; execution logic needs a code adapter. Do not
  advertise or attempt datapack-defined custom behaviour.
- ISS and Better Spellcasting must never become hard dependencies of this repo -
  the battlewill system stands alone.
- Its ability registration crosses into `tensura-ability-core` contracts: schema
  changes there are `breaking` and must be coordinated with that repo's issues.