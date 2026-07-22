# Galactic Wars: Clone Wars

Galactic Wars: Clone Wars is an unofficial, GPL-3.0-only Architectury mod for Minecraft 26.2 on Fabric and NeoForge. It focuses on faction armies, working settlements, planetary travel, vehicles, Force progression, quests, trading, and conquest.

This is a clean-break conversion. Existing KingdomWars-Middle-Earth worlds and registry IDs are not compatible; start a fresh world.

## Installation

1. Install Java 25 and Minecraft 26.2 with either Fabric Loader 0.19.3+ or NeoForge 26.2.0.25-beta+.
2. Install the matching `galacticwars-fabric` or `galacticwars-neoforge` JAR.
3. Install the loader-matched Architectury API 21.0.4+, GeckoLib 5.5.3+, SmartBrainLib 2.0.x, and YACL 3.9.5+ builds. Fabric also requires Fabric API and Fabric Language Kotlin; NeoForge requires Kotlin for Forge.
4. Remove earlier KingdomWars-Middle-Earth builds and start a fresh world.
5. Keep the same loader, Galactic Wars build, and dependency versions on every multiplayer client and server.

## Requirements

- Minecraft 26.2
- Java 25
- Architectury API 21.0.4 or newer compatible release
- Fabric Loader 0.19.3, Fabric API 0.155.2+26.2, and Fabric Language Kotlin 1.13.13+kotlin.2.4.10; or NeoForge 26.2.0.25-beta and Kotlin for Forge 6.3.0
- GeckoLib 5.5.3 or newer compatible release
- SmartBrainLib 2.0.x for the selected loader
- YACL 3.9.5 or newer compatible 26.2 release for the selected loader

## Controls

- Place a Command Center to open the faction picker. Choosing a faction atomically creates and pledges your kingdom; the Command Center, recruitment rules, alignment, and progression all use that saved choice.
- Interact with a recruit to open the command screen. Hire first, then choose combat orders, formations, a worksite, storage, a profession, or construction controls.
- Use a blaster to fire; each accepted shot consumes one Energy Cell, applies weapon durability and a short server-authoritative cooldown.
- Interact with an active Command Center for the keyboard-focusable Overview, Campaign, Construction, Squads, Workforce, Kingdom, Diplomacy, and Storage dashboard. Sneak-interact for planetary navigation.
- Craft a Tactical Command Marker and use it on an entity or block before issuing explicit attack, move, rally, patrol, worksite, or storage orders. Prepare a Blueprint Projector in the Construction tab, then place the projection in-world; builders withdraw real blocks from Command Center or linked storage.
- Press `G` to open the rebindable Field Command screen when you are near your squads. Select up to eight commandable squads and issue battlefield movement, combat, formation, and patrol orders through the same server-authoritative army state used by the Command Center.
- After pledging and completing a Forward Base, sneak-use your active Command Center to open planetary navigation. Craft and carry a Hyperspace Navigator to reopen the console away from home. Every jump still requires your active Command Center and paid upkeep, and prepares a reusable non-destructive arrival platform.
- Deploy a fabricated vehicle kit in-world. Use movement keys to drive, Jump/Crouch to climb or descend in flight vehicles, and `R` to fire the mounted weapon. Use an Energy Cell to refuel and Duracrete to repair an authorized vehicle.
- Player classes activate their two equipped class abilities with `V` and `B`. Jedi, Sith, and Nightsister paths equip three active Force abilities at their faction shrine and activate them with `Z`, `X`, and `C`. The HUD reports energy, charge/channel state, and per-slot cooldowns; targeting, costs, replay protection, PvP policy, and effects are server authoritative.
- Follow the objective marker for the next relevant worksite, merchant, outpost, mission, or conquest target. It reports direction and distance in the target dimension and names the dimension when travel is required.
- Dedicated-server gameplay policy lives in `config/galacticwars-server.properties`. Operators can reload it with `/galacticwars reloadpolicy`; connected clients receive a read-only policy snapshot. YACL edits only local HUD and accessibility preferences such as HUD position/scale, effect intensity, particles, camera shake, contrast, redundant status, and narration.

## Core gameplay loop

1. Build a Command Center and pledge to the Republic, Separatists, Mandalorians, Hutt Cartel, or Nightsisters.
2. Earn physical Credit Chips through faction objectives, work, exploration, and trading.
3. Recruit soldiers, droids, specialists, and workers. Give squads follow, hold, move, protect, attack, patrol, and formation orders.
4. Assign workers to real farms, salvage zones, mines, storage, deliveries, and construction projects.
5. Complete a Forward Base, Barracks, Supply Depot, Moisture Farm, Salvage Yard, and Mine to expand housing, logistics, travel, vehicles, and command capacity.
6. Travel between Tatooine, Geonosis, Kamino, and Coruscant; complete faction missions and Force training, then obtain a vehicle through fabrication, requisition, or faction trade.
7. Assault and hold regional objectives with troops and a vehicle, then defend them from visible counterattacks without overwriting protected player builds.

Credits, unlocks, quest milestones, vehicles, Force choices, and conquest use an idempotent server-side progression coordinator: duplicate packets or reconnects must not duplicate charges or rewards.

## Current development status

The mod is in **late alpha**. The fixed 1.0 launch boundary is five factions, four planets, five vehicles, and three Force traditions. Its server-authoritative gameplay vertical is implemented and extensively automated, but a feature is only considered stable after its player-facing path and the relevant runtime and manual acceptance evidence exist.

### Finished: connected runtime foundation

- [x] **Cross-loader foundation:** the `galacticwars` namespace, Fabric and NeoForge entry points, generated metadata, versioned networking, atomic datapack reloads, strict cross-content validation, and last-known-good reload recovery are connected.
- [x] **Survival onboarding:** recipes and advancements lead to the Command Center; faction selection creates the kingdom and pledge atomically; the guided Starter Camp flow supports orientation, deployment, retry, builder reassignment, and pack-up.
- [x] **Factions, units, and classes:** five factions, 21 data-driven military unit definitions, and 21 player-selectable classes are wired to faction requirements, equipment, attributes, hiring costs, active abilities, and persistence.
- [x] **Recruit and army play:** hiring, equipment and cargo, commanders, explicit squad/member selection, formations, follow/hold/move/protect/attack/patrol/rally orders, field-command batches, marching, loaded/virtual movement, unload recovery, ranged and melee AI, and friendly-fire rules have runtime paths.
- [x] **Settlements and workforce:** seven blueprints including the Starter Camp, physical material construction, housing and upkeep, worker professions, farms/mines/salvage/cooking/building, storage endpoints, conserved inventory transfer, courier routes, blocked-state feedback, and reload-safe work orders are implemented.
- [x] **Combat and equipment:** four blaster families, six lightsaber colors, faction equipment families, server-authoritative ammunition/cooldowns, blaster heat HUD feedback, projectile combat, lightsaber guarding/deflection, and recruit combat behavior are implemented.
- [x] **Campaign guidance and reachability:** 24 quest records cover five three-chapter faction campaigns plus nine Force-training quests. Every faction path now discloses Supply Depot construction before vehicle acquisition, the Mandalorian path requires the same three deliveries as advanced trading, and a reachability validator rejects hidden prerequisites. The Command Center and Force Shrine expose localized titles, briefings, prerequisites, rewards, next physical actions, target context, and active progress; a dimension-aware HUD marker guides the player to the next authoritative target.
- [x] **Embodied missions and rewards:** all 15 faction chapters use versioned secure-camp, supply/escort, or assault/hold mission definitions with validated references. The server persists the active target, attempt, phase, encounter wave, hold progress, failure reason, and retry deadline. Players must bring the required worker/squad/vehicle to the marked site, defeat the hostile wave, and hold it; death or abandoning an active encounter fails safely into a bounded cooldown. Restart recovery, retry activation, replay-safe completion, and duplicate-reward rejection are connected, and the Command Center exposes the active mission state.
- [x] **Purposeful class ranks:** combat contribution, squad support, and mission completion award bounded class XP. Data-driven milestones unlock the signature ability at rank 1, a secondary/passive at rank 3, and efficiency, cooldown, and potency improvements at ranks 5, 7, and 10. The class screen and HUD show XP, the next milestone, unlocked effects, and specific lock reasons; Force specialists still progress at shrines.
- [x] **Force traditions:** Jedi, Sith, and Nightsister progression provides 39 tree nodes and 31 active abilities with shrine interaction, nine visible training quests, rank/point progression, loadouts, press/charge/channel input, energy, cooldowns, world effects, block restoration, NPC protection, and authoritative PvP/physics policy.
- [x] **Planets, sites, and travel:** Tatooine and Geonosis use noise-based generation; Kamino is an ocean/platform world; Coruscant uses an authored urban foundation. Every planet definition includes arrival, economy, and contested sites, and travel prepares non-destructive themed infrastructure. Respawn binding, return travel, failure-atomic squad transfer, cargo preservation, outpost registration, and navigation guidance are connected. Terrain changes apply only to new chunks, so a fresh world is recommended for the stable release.
- [x] **Vehicles:** the BARC Speeder, AT-RT, STAP, AAT, and LAAT Gunship can be fabricated, deployed, boarded, driven or flown, fueled, armed, damaged, repaired, persisted, and destroyed under server ownership and seat rules.
- [x] **Economy, diplomacy, warfare, and endgame:** physical Credit Chips, treasury operations, recruitment and upkeep, 13 launch trade offers, kingdom roles/permissions/relations, and a documented vehicle route for every faction connect workforce output to construction, army supply, fabrication, requisition, and trade. Four conquest regions have distinct encounter/reward identities; captures schedule visible NPC counterattacks, failed defenses return a region to contested control without deleting protected builds, and active sieges/counterattacks appear in the Command Center with participant and progress context.
- [x] **Configuration authority and accessibility:** gameplay policy is isolated in an authoritative server file, synchronized read-only to clients, and reloadable only by operators. Local YACL settings cannot imply control over dedicated-server PvP, friendly fire, or Force physics. HUD layout/scale, effect intensity, particle density, camera shake, contrast, redundant status, and narration preferences are stored separately.
- [x] **Presentation and provenance foundation:** fixed interface text, quest guidance, objective names, subtitles, and status messages have `en_us` keys with automated translation-completeness coverage. Registered sound identities cover blasters, lightsabers, Force use, vehicles, travel, construction, missions, and planet ambience through documented vanilla aliases; faction equipment, NPCs, weapons, planets, effects, GUIs, and vehicle models/textures remain manifest-validated and provenance-tracked.
- [x] **Automated verification:** the current tree contains 139 executable `*Test.java` harnesses and 59 required NeoForge GameTests covering reachability, mission/content references, mission failure/retry/reload, translations, class milestones, config separation, bounded codecs, persistence, networking, multiplayer authority scenarios, NPC behavior, menus, combat, travel, vehicles, Force effects, counterattack state, conquest, and all five campaign paths. `runHarnesses` and `runGameTestServer` are green; `buildAll` remains the cross-loader artifact gate.

### Needs work before 1.0

- [ ] **Mission acceptance and tuning:** complete each of the 15 faction-specific mission variants in fresh Survival, including death, abandoning the hold area, chunk unload, restart, retry, and reconnect checkpoints. Tune wave size, site radius, hold duration, retry time, and support requirements from that evidence without weakening server authority or reward replay protection.
- [ ] **Planet and warfare acceptance:** visually review new-chunk terrain and all 12 authored planet sites, give the four conquest regions unmistakable encounter/reward identities in play, and verify navigation among arrival sites, merchants, outposts, missions, and beacons. Exercise counterattack victory/failure and siege notifications with real kingdom members, including protected player construction.
- [ ] **Presentation completion:** replace the temporary vanilla sound aliases with original or compatibly licensed recordings where appropriate, preserving provenance and subtitles. Finish held weapon poses, recruit combat/work animation coverage, vehicle cameras, construction/mission feedback, and clear failure feedback.

- [ ] **Real dedicated-server multiplayer pass:** run two actual clients against a dedicated server and record invitations, roles, squad authority, stale/replayed actions, PvP and friendly-fire policy, trades, vehicle seats, Force targeting, reconnects, and cross-dimension travel on both loaders. GameTest mock players are useful coverage, but they are not this acceptance test.
- [ ] **Fresh survival completion:** record five command-free, non-Creative playthroughs from the Command Center recipe to persistent campaign victory, one per faction, including save/reload checkpoints at the starter camp, settlement, mission, travel, vehicle, Force-training where applicable, conquest, and counterattack stages. Fix any discoverability or resource dead ends found.
- [ ] **Client visual and accessibility acceptance:** verify the Command Center, Starter Camp, recruit/loadout, class/Force, merchant, navigation, and field-command screens at common resolutions and GUI scales; check keyboard-only use, focus order, narration, contrast/color reliance, redundant cues, all HUD positions/scales, effect/particle intensity, camera shake, and HUD overlap.
- [ ] **Balance and soak testing:** tune Credit, upkeep, recruitment, worker throughput, construction costs, campaign rewards, Force energy/cooldowns, vehicle health/fuel/damage, and conquest pacing. Soak-test large settlements, several squads, chunk unload/reload, repeated planet jumps, and long dedicated-server sessions for leaks, duplication, stuck AI, or save growth.
- [ ] **Release packaging:** smoke-test the produced Fabric and NeoForge JARs with the documented dependency matrix on clean client/server installs, confirm configuration defaults and failure messages, produce a changelog and known-issues list, and archive the acceptance evidence.

### After the first stable release

- [ ] Add new planets, unit families, vehicles, bosses, and longer campaigns without expanding the 1.0 acceptance boundary.
- [ ] Explore capital ships and free-flight space combat only after the embodied recruit-command-build-fight loop is stable and performant.
- [ ] Add languages beyond complete `en_us` coverage.

## Development

```powershell
.\gradlew.bat clean buildAll
.\gradlew.bat runHarnesses
.\gradlew.bat runGameTestServer
.\gradlew.bat :fabric:runClient
.\gradlew.bat :neoforge:runClient
```

`buildAll` produces loader-specific JARs under `fabric/build/libs` and `neoforge/build/libs`. Executable `*Test.java` harnesses are part of `check`; NeoForge hosts the shared Minecraft GameTest suite. Generated or edited textures must satisfy `docs/galacticwars-asset-manifest.json`, and generation provenance is recorded in `docs/galacticwars-asset-provenance.md`.

## Licensing and fan-project notice

This is an unofficial fan project and is not affiliated with or endorsed by Lucasfilm or Disney. Star Wars names identify the fan setting; newly introduced code, textures, models, and writing are project-owned originals. GPL-derived historical code remains GPL-3.0-only and its required attribution is retained in `NOTICE.md`.

Authorized derivative code from the local `talhanation/recruits` checkout is used only where recorded in `docs/authorized-source-intake.md`; each retained or adapted unit remains provenance-tracked. Its legacy networking, global managers, medieval assets, and unrelated gameplay systems are not included.
