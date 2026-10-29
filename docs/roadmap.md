# Roadmap - tensura-battlewills

Generated from and kept in sync with this repository's issues. The authoritative
status is always the issue list. The cross-repository requirements inventory
(REQ IDs) lives in `tensura-ability-core/docs/requirements.md` - this roadmap
references those IDs rather than duplicating their status.

## Phase 0 - Organisation and repository foundation

- [x] Repository created from `neoforge-mod-template` (bootstrap PR #1 merged)
- [x] Issue forms (feature / bug / investigation), labels, branch rulesets applied

## Phase 1 - Requirements and feasibility (spikes)

- [ ] #2 - Battlewill baseline in Tensura + datapack-versus-code-adapter boundary (SPIKE-bw-1)
- [ ] #3 - Resource model: cost source, conversion, persistence (SPIKE-bw-2)
- [ ] #4 - Repo-F boundary: dedicated Better Combat adapter vs shared bridge, DEC-03 (SPIKE-bw-3)

Each spike closes only with verified findings **and** its implementation issues
created (investigation form contract).

## Phase 3 - Battlewill fundamentals

One implementation issue per requirement, created as spikes close:
REQ-BW-01 (weapon-style definitions incl. unarmed), REQ-BW-02/03/04 (actives,
enhancements, passives), REQ-BW-05 (resource conversion and costs), REQ-BW-06
(progression/restrictions), REQ-BW-07 (datapack schema + loader + adapter
registry), REQ-BW-08 (native activation), REQ-BW-09 (hotbar integration).

Content issues split one per technique group once SPIKE-bw-1 records whether
battlewills are new content or Tensura-native (scope decision).

## Phase 4 - Combo integration

- REQ-BR-01/10 payloads for battlewills land here or per DEC-03 outcome (this
  repo vs the bridge repo).

## Hard constraints

- REQ-BW-11: ISS and Better Spellcasting are never hard dependencies - every
  integration is a soft hook; the battlewill system must stand alone.
- Datapack rows without a matching execution adapter are inert with a
  diagnostic, never a crash (REQ-XREF-04, REQ-TEST-04).

## Phase 6 - Compatibility and release readiness

- Datapack validation GameTests (schema + referenced ids), optional-mod
  present/absent combination tests, persistence tests for progression state.
