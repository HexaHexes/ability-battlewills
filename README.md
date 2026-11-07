# Ability: Tensura

Registers **Tensura: Reincarnated**'s Battlewills - the pack's non-magical
combat arts, martial techniques bound to how you fight, from sword and spear
discipline to unarmed brawling - as native, datapack-first abilities:
discovered through Ability Core, unlocked through Tensura's own learning,
and activated natively first, with the shared Ability Hotbar and combo
bridges as optional seams rather than requirements. Built for the Tensura
RPG pack.

## What it does

- **Catalogs** - all 23 native battlewills plus the skill and magic
  catalog (332 native abilities from their category tags) registered as
  Ability Core definitions with discovery and metadata.
- **Unlock gate** - every registered ability is gated on the Tensura
  unlock (ManasCore's learned-state read), and unlearnt abilities are
  removed from the player's bars as they are forgotten.
- **Native activation parity** - a shared activation service resolves
  preset slots server-side, so native casting and the ability pipeline
  agree on what fires; battlewills activate natively first, with the hotbar
  seam as an addition.
- **Mode switching** - the hotbar's mode keybind intent routes through
  Ability Core's mode-change contract into ARR's own cycle packet
  (per-slot mode presets included), so both paths move the same native
  state (the keybind and its transport ride the Ability Hotbar). 
- **Datapack-first** - definitions, tags, progression and restrictions
  live in datapack data, so content is editable without code. Iron's
  Spells and Better Spellcasting are not hard dependencies.
- The rest of the plan - the style system, passives, weapon enhancements,
  the additive-art schema and the Ability Hotbar provider adapter - is
  tracked in this repository's open issues.

## Aura and magicules (resources)

Battlewills are priced in **aura** - each art carries its native
`auraCost` (200 by default), and Tensura's config owns every number in
this system: this pack reads the native state and never re-implements
any of it. Aura refills **from magicule, natively**: hold the battlewill
(`tensura:battlewill`) and, every one and a half seconds while your aura
isn't full, a share of your maximum magicule converts into the same
amount of aura - the share defaults to **1% of your max magicule** and
**doubles once the art is mastered** (`percentage` in Tensura's battlewill
config, `mastered = percentage / 50` against the unmastered
`percentage / 100`), with the cast sound and pulse. Too little magicule
and nothing converts - the red "lack magicule" line says so. Our bars
project this same native state for affordability, and our activation
paths never pre-charge magicule for a native art: the native pipeline
pays, exactly once.

## Dependencies

| | Mod | Why |
|---|---|---|
| Required | Minecraft | - |
| Required | NeoForge | - |
| Optional | Ability Core | The ability contracts battlewills register against: discovery, metadata, activation (`[1.0.0-alpha.42,)` in the toml); without it the mod carries only its datapack data |
| Optional | Tensura: Reincarnated | The battlewills themselves and the unlock state the gate reads |
| Optional | ManasCore | SkillAPI: the unlock gate's learned-state read |
| Optional | Ability Hotbar | The shared hotbar seam (the provider adapter is tracked in #42); absent = native activation alone |
| Optional | Tensura - Better Spellcasting Bridge | Battlewills fire from weapon combos; runtime-gated, absent = no combo hooks |

## Credits

- Built on [NeoForge](https://neoforged.net/).
- Derived from [neoforge-mod-template](https://github.com/HexaHexes/neoforge-mod-template).

## License

GNU Lesser General Public License v2.1 - see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.
