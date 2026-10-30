# Mod Health Issue List + NeoForge/Fabric Update Plan (2026-10-30)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Record the 2026-10-30 health audit findings and update the mod's loader/dependency stack to the latest versions verified against upstream Maven/CurseForge on 2026-10-30, staying on the Minecraft 26.2 line.

**Architecture:** Four parallel `explore` subagents audited build/code/assets/tests-docs; versions were verified live (Maven metadata for NeoForge/Fabric-loader/Fabric-API/FLK/Kotlin/SBL/YACL/GeckoLib/Architectury; CurseForge for Architectury-API/Fabric-API/GeckoLib/SBL/YACL/KFF). The 26.2-line update is applied now; the MC 26.3 port is scoped as follow-up work.

**Tech Stack:** Java 25, Gradle 9.5.1, Architectury Loom-no-remap 1.17.491, Architectury API, NeoForge, Fabric Loader/API, Kotlin 2.4.x, GeckoLib, SmartBrainLib, YACL.

## Global Constraints

- Stay on Minecraft 26.2 for this update; do NOT jump to MC 26.3 (SDL3 windowing, MultiDrawIndirect terrain rendering, datapack v121/resource-pack v97.1 changes need a dedicated port).
- Keep `org.gradle.configuration-cache=false` until the Architectury production-transform issue is resolved upstream.
- `runHarnesses`, `runGameTestServer`, `buildAll`, and `git diff --check` remain the release gates; green automation does not replace fresh-Survival or two-client acceptance.
- Do not describe a catalog/policy/source-presence test as completed gameplay until its player-facing runtime path is wired and tested (AGENTS.md).

---

## Part A — Version matrix (current -> update, verified 2026-10-30)

| Artifact | Current | Latest (26.2 line) | Source |
|---|---|---|---|
| Minecraft | 26.2 | 26.2 KEEP (26.3 exists, separate port) | mcreference / MC wiki |
| NeoForge | 26.2.0.25-beta | **26.2.0.93** | maven.neoforged.net metadata (92 releases on 26.2 line) |
| Fabric Loader | 0.19.3 | **0.20.1** | maven.fabricmc.net metadata |
| Fabric API | 0.155.2+26.2 | **0.161.0+26.2** | maven.fabricmc.net metadata (37 builds on 26.2 line) |
| Architectury API | 21.0.4 | **21.1.12** | maven.architectury.dev + CurseForge (Oct 9 2026) |
| GeckoLib | 5.5.3 | **5.5.5** unified (NeoForge-only 5.5.6 exists; Fabric latest is 5.5.5) | cloudsmith metadata |
| SmartBrainLib | 2.0.0 | **2.0.3** | cloudsmith + CurseForge (Oct 4 2026) |
| YACL | 3.9.5+26.2 | **3.9.7+26.2** | maven.isxander.dev + CurseForge (Sep 20 2026) |
| Kotlin (plugin) | 2.4.0 | **2.4.21** (matches FLK) | Maven Central metadata |
| Fabric Language Kotlin | 1.13.13+kotlin.2.4.10 | **1.14.2+kotlin.2.4.21** | maven.fabricmc.net metadata |
| Kotlin for Forge | 6.3.0 | 6.3.0 KEEP (latest for 26.2; 6.4.0 is 26.3-only) | CurseForge |
| Coroutines | 1.11.0 | 1.11.0 KEEP (latest stable) | Maven Central metadata |
| JUnit Jupiter | 5.12.2 | **5.14.4** + pin `junit-platform-launcher:5.14.4` | Maven Central metadata |

MC 26.3 reference (follow-up port, NOT this update): MC 26.3 (Sep 15 2026), NeoForge 26.3.0.99, Fabric API 0.165.0+26.3, Architectury 22.0.5, GeckoLib 5.5.7, SBL 2.0.3, YACL 3.9.7+26.3, KFF 6.4.0, Loader 0.20.1.

## Part B — Prioritized issue list (from subagent audit)

### P0 — Fix before any release branch
- [ ] GameTest count drift: code registers 91 (`ModGameTests.java:521-630`), README claims 92. Correct the count and assert it in a harness.
- [ ] CI only runs `./gradlew build` (ubuntu-only, no `buildAll`, no GameTests, no `diff --check`, no Fabric coverage, no artifacts).
- [ ] Fabric has zero automated runtime coverage (no harnesses/GameTests wiring; root `runHarnesses` is NeoForge-only).
- [ ] Unified `geckoLibVersion` blocks per-loader latest (Fabric max 5.5.5 vs NeoForge 5.5.6).

### P1 — Code health (correctness risks)
- [ ] `KingdomSavedData.java` (2066 lines, 15+ maps) and `GalacticRecruitEntity.java` (8389 lines) are god classes; `ModGameTests.java` (13634 lines) is a god test.
- [ ] `ForceWorldEffectService` holds a class-level `synchronized` lock across targeting/execution/IO (`tickChannels`) — contention + tick-stall risk; move to `ConcurrentHashMap` + per-player locks.
- [ ] `GalacticNetwork.handleResearch` silently swallows unknown actions (`default -> {}`); `technicianId.orElseThrow()` throws on malformed client packets on the server thread.
- [ ] `ConstructionProjectService.findLoadedRecruit` falls back to O(n) entity scan; hall-authority lookup (`instanceof CommandCenterBlockEntity`) is duplicated ~22x.
- [ ] Vehicle tuning (`GalacticVehicleEntity`) and army/Force ranges are hardcoded literals instead of datapack/balance config.

### P2 — Assets / presentation
- [ ] 23 item models/textures have no `en_us` key (20 faction armor pieces + `beskar_vibroblade` + `force_light`/`force_dark`).
- [ ] `blueprint_site_anchor/loot` use `minecraft:block/air` placeholders with no model, loot table, or lang key.
- [ ] Orphaned 512px `_hq` blaster sources ship in the JAR unreferenced (manifest violation + bloat).
- [ ] Duplicate NPC atlases (`clone==arc`, `phase_i_clone==phase_i_arc`) break the distinct-hash gate; vanilla equipment layers are 4-color placeholders.
- [ ] All 8 sounds are vanilla aliases; `faction_command_post` reuses command-center art; 7 field kits/tools have models but no recipe.

### P3 — Docs / process
- [ ] Stale QA docs (`visual-qa/README`, old plan docs) cite 136/144 tasks and 55 GameTests vs current 186/91.
- [ ] `LibraryDependencyIntegrationTest` couples harness to build-artifact order (fails standalone); pins versions that will drift on bump.
- [ ] Harness reporting is `println`-only (no JUnit XML, no per-test timeout); `failOnNoDiscoveredTests=false` makes `test` vacuous.
- [ ] No version catalog (`libs.versions.toml` missing); 14 duplicate snake_case aliases in `gradle.properties`; config-cache permanently off with no tracking issue.

## Part C — Update tasks (this change)

### Task 1: Bump 26.2-line versions in `gradle.properties`
**Files:** Modify: `gradle.properties:19-47`
- [ ] Step 1: Set `neoForgeVersion=26.2.0.93`, `fabricLoaderVersion=0.20.1`, `fabricApiVersion=0.161.0+26.2`, `architecturyApiVersion=21.1.12`, `geckoLibVersion=5.5.5`, `smartBrainLibVersion=2.0.3`, `yaclVersion=3.9.7+26.2`, `kotlinVersion=2.4.21`, `fabricLanguageKotlinVersion=1.14.2+kotlin.2.4.21` (and matching `snake_case` aliases `neo_version`, `architectury_api_version`, `geckolib_version`, `smartbrainlib_version`, `yacl_version`).
- [ ] Step 2: Update `neoforge/build.gradle.kts:107-108` JUnit to `5.14.4` and pin `junit-platform-launcher:5.14.4`.
- [ ] Step 3: Run `git diff --check`.
- [ ] Step 4: Commit (only when user requests): `Update to latest 26.2-line NeoForge and Fabric stack`.

### Task 2: Sync user-facing version references
**Files:** Modify: `README.md:9-23` (Installation + Requirements version pins)
- [ ] Step 1: Update README pins to NeoForge 26.2.0.93, Loader 0.20.1, Fabric API 0.161.0+26.2, Architectury 21.1.12, GeckoLib 5.5.5, SBL 2.0.3, YACL 3.9.7, FLK 1.14.2+kotlin.2.4.21.
- [ ] Step 2: Run `git diff --check`.

### Task 3: Verify
- [ ] Step 1: Run `./gradlew.bat :common:compileJava :fabric:compileJava :neoforge:compileJava` (fast gate).
- [ ] Step 2: Run `./gradlew.bat buildAll` (dual-loader JARs) when time allows; record task counts and failures verbatim.
- [ ] Step 3: Do NOT claim success without pasting command output (verification-before-completion).

## Execution Handoff

Plan saved to `docs/superpowers/plans/2026-10-30-neoforge-fabric-update.md`. Recommended: Subagent-Driven (one fresh subagent per P0/P1 issue) or inline execution of Task 1-3 now.
