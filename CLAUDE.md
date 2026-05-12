# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Keep replies extremely concise. No filler.

## Code Rules (non-negotiable)

- No comments unless the WHY is genuinely non-obvious (hidden constraint, workaround, surprising invariant). Never explain WHAT the code does.
- No multi-line comment blocks or docstrings.
- No error handling for scenarios that cannot happen. Trust internal code and framework guarantees. Only validate at real system boundaries (user input, external APIs).
- No features, refactoring, or abstractions beyond what the task requires. Three similar lines > premature abstraction.
- No half-finished implementations. No TODOs left in code.
- No feature flags or fallbacks for hypothetical future requirements.
- Bug fix = fix the bug only. Do not clean up surroundings.

## What This Repo Is

`api-cp-caseadmin-case-urn-mapper` is an **OpenAPI-first API spec library**. It has no runnable application — its output is a published JAR containing generated Spring interfaces and Lombok DTOs that downstream services depend on.

The single endpoint is `GET /urnmapper/{case_urn}`, which maps a Case URN to a Case ID.

## Commands

```bash
# Full build (generates code, compiles, runs tests)
./gradlew build

# Set an explicit version (required in CI; defaults to 0.0.999 locally)
./gradlew build -DAPI_SPEC_VERSION=<version>

# Regenerate DTOs and Spring interfaces from the OpenAPI spec
./gradlew openApiGenerate

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests 'uk.gov.hmcts.cp.config.OpenApiObjectsTest'

# Run a single test method
./gradlew test --tests 'uk.gov.hmcts.cp.config.OpenApiObjectsTest.myTest'

# Code quality
./gradlew pmdMain          # PMD static analysis (not run automatically during build)
./gradlew spotlessCheck    # Check formatting
./gradlew spotlessApply    # Auto-fix formatting
./gradlew check            # Tests + JaCoCo coverage report

# Publish to local Maven cache (needed before a dependent service can build against local changes)
./gradlew publishToMavenLocal

# Lint OpenAPI spec
spectral lint "src/main/resources/openapi/*.{yml,yaml}"
```

## Architecture

### Code Generation Pipeline

The source of truth is `src/main/resources/openapi/openapi-spec.yml`. Running `./gradlew openApiGenerate` (which also runs automatically before `compileJava`) produces:

- `build/generated/src/main/java/uk/gov/hmcts/cp/openapi/api/` — Spring `@RequestMapping` interfaces, one per OpenAPI tag
- `build/generated/src/main/java/uk/gov/hmcts/cp/openapi/model/` — Lombok DTOs with `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@JsonInclude(NON_NULL)`

**Never edit files under `build/generated/` directly.** All model/interface changes go through the OpenAPI spec.

Key generator settings (in `gradle/openapi.gradle`):
- `interfaceOnly: true` — no controllers generated, only interfaces
- `OffsetDateTime` → `Instant` type mapping
- `openApiNullable: false` — avoids `JsonNullable` wrapper types
- `generatedConstructorWithRequiredArgs: false` — prevents conflict with Lombok's `@AllArgsConstructor`

### JSON Schema

`src/main/resources/openapi/schema/` holds JSON Schema files for response objects. Each schema has a paired `*.example.json` file validated against it in CI. The OpenAPI spec references these schemas via `$ref`.

### Tests

`src/test/java/uk/gov/hmcts/cp/config/OpenApiObjectsTest.java` is the only test class. It uses reflection to assert that the generator produced the expected fields and methods — this acts as a contract test against accidental spec/generator changes.

### Publishing

On push to `main`, CI publishes the JAR to:
- GitHub Packages (`maven.pkg.github.com/$GITHUB_REPOSITORY`)
- Azure Artifacts (`pkgs.dev.azure.com/hmcts/Artifacts/_packaging/hmcts-lib/maven/v1`)

Local publishing: `./gradlew publishToMavenLocal` (requires no credentials).

### Gradle Configuration

Shared config is split across `gradle/` scripts applied in `build.gradle`:

| File | Purpose |
|---|---|
| `java.gradle` | Java 25 Temurin toolchain; `-Xlint:unchecked -Werror` (warnings are errors) |
| `openapi.gradle` | OpenAPI Generator config; wires `openApiGenerate` before `compileJava` |
| `test.gradle` | JUnit Platform, JaCoCo, fail-fast, CI-friendly reporting |
| `pmd.gradle` | PMD via `.github/pmd-ruleset.xml`; excludes generated code; not run during standard build |
| `repositories.gradle` | Maven Central + Azure Artifacts resolution; GitHub Packages + Azure Artifacts publishing |
| `dependency.gradle` | Rejects unstable (RC/alpha/beta) dependency upgrades |
| `jar.gradle` | Includes CHANGELOG.md and CycloneDX SBOM (`bom.json`) inside the published JAR |

### CI Workflows

- **`ci-draft.yml`** — triggers on PR/push to `main`; generates a draft version, runs tests, publishes draft artefact and OpenAPI spec to SwaggerHub
- **`ci-released.yml`** — triggers on GitHub release; publishes a release-tagged artefact
- **`lint-openapi.yml`** — on PRs: Spectral lint, JSON schema lint (`jsonlint`), and AJV schema-vs-example validation; also checks that internal HMCTS domain URLs are not in the spec
- **`code-analysis.yml`** — SonarCloud analysis
- **`codeql.yml`** — GitHub CodeQL security scan

## Key Constraints

- Java 25, Spring Boot 4.0.x, Jakarta EE (not `javax`)
- `-Werror` means all compiler warnings fail the build — suppress or fix them explicitly
- OpenAPI 3.x only (no Swagger v2)
- Do not reference internal HMCTS domains (`cjscp.org.uk`, `service.gov.uk`, `justice.gov.uk`, `hmcts.net`, `ejudiciary.net`) in the OpenAPI spec — CI will reject them
- Run `/openapi-spec-reviewer` skill when authoring or reviewing the OpenAPI spec