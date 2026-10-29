# Tensura Battlewills

Tensura Battlewills adds the non-magical combat arts of the Tensura RPG pack.
Battlewills are martial techniques bound to how you fight, from sword and spear
discipline to unarmed brawling, defined as datapack content.

## What it does

*(Design phase, no release yet. The open issues on this repository hold the
full plan, research tables and open design decisions.)*

- **Weapon styles** - sword, spear and polearm, axe and heavy weapon, ranged,
  shield and defensive techniques, and unarmed combat as its own style.
- Techniques are active abilities you trigger, weapon enhancements you apply,
  and passives that shape the fight.
- **Progression and cost** - abilities carry resource costs, progression and
  restrictions, all readable from datapack definitions.
- Battlewills use native activation first, with hooks for the shared ability
  hotbar and for combo bridges. Iron's Spells and Better Spellcasting are not
  hard dependencies.

## Dependencies
| | Mod | Why |
|---|---|---|
| Required | Minecraft / NeoForge | 1.21.1 / 21.1.259 |
| Required | Tensura Ability Core | The ability contracts battlewills register against |
| Optional | Tensura Ability Hotbar | Battlewills appear in the shared hotbar |
| Optional | Tensura - Better Spellcasting Bridge | Battlewills fire from weapon combos |

When the first alpha ships, drop the jar in `mods/` alongside its required
dependencies.

## Credits
- Built on [NeoForge](https://neoforged.net/).
- Part of the [Tensura RPG](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated) pack ecosystem.

## License

GNU Lesser General Public License v2.1 - see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.