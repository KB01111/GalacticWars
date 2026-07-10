# Repository Guidelines

## Project Structure & Module Organization

This is a Java 25 NeoForge mod. Production code lives in `src/main/java/middleearth/lotr/warmod`, grouped by gameplay domain such as `army`, `faction`, `recruitment`, `settlement`, and `workforce`. Mod metadata is generated from `src/main/templates`; static assets, data packs, mixins, models, textures, and localization belong under `src/main/resources`. Keep resource paths namespaced with `kingdomwarsmiddleearth`. Test harnesses mirror the production packages in `src/test/java`. Design notes and implementation plans are stored in `docs/superpowers`. Treat `build/`, `.gradle/`, and `run/` as generated local state.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper (Windows examples shown):

- `.\gradlew.bat build` compiles sources, processes resources, and runs the standard verification lifecycle used by CI.
- `.\gradlew.bat testClasses` compiles the custom test harnesses.
- `.\gradlew.bat runClient` launches a development Minecraft client.
- `.\gradlew.bat runServer` starts a local server with `--nogui`.
- `.\gradlew.bat runData` regenerates data into `src/generated/resources`.
- `.\gradlew.bat runGameTestServer` launches NeoForge GameTests when registered.

Unix-like environments should use `./gradlew` instead.

## Coding Style & Naming Conventions

Follow the existing Java style: four-space indentation, UTF-8, braces on the declaration line, and one public top-level type per file. Use `UpperCamelCase` for classes and records, `lowerCamelCase` for methods and variables, and lowercase package names. Prefer small domain types and immutable value objects. Name resource files and registry identifiers in lowercase `snake_case`; keep Java packages aligned with their directories. No formatter or linter is configured, so match nearby code and keep imports organized.

## Testing Guidelines

Tests are dependency-light executable classes named `*Test.java`, with a `public static void main` and explicit assertions; they are not JUnit tests. Add tests beside the matching domain package and print `<ClassName> passed` on success. Gradle currently compiles these harnesses but does not discover them as JUnit tests, so run relevant main classes explicitly after `testClasses`, then run `.\gradlew.bat build` before submitting. No coverage threshold is configured.

## Commit & Pull Request Guidelines

Recent commits use short, imperative subjects such as `Add configurable recruit work areas` and `Fix recruit GUI render extraction`. Keep each commit focused. Pull requests should explain behavior and scope, link related issues or plan documents, list verification commands, and include screenshots for GUI, model, or texture changes. Ensure CI’s Java 25 Gradle build passes.

## Licensing & Source Use

Follow `NOTICE.md`: LOTR-derived work must remain GPL-3.0-only compatible. Treat `talhanation/recruits` only as a behavioral reference; do not copy its code or assets without explicit compatible permission.
