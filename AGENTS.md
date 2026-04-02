# AGENTS.md

## Frontend

The web interface lives in `graylog2-web-interface/` and has its own conventions. Before modifying frontend code, you must read `graylog2-web-interface/AGENTS.md` and follow its conventions.

## Build Commands

```bash
# Quick verification — compiles Java, skips frontend and javadoc
./mvnw compile -pl graylog2-server -Dskip.web.build=true -Dmaven.javadoc.skip=true

# Full, clean compilation (includes frontend, runs all checks)
./mvnw clean test-compile
```

**Note:** If working from the `graylog-project-internal` meta-project, run Maven commands from that root directory instead.

## Running Tests

```bash
# Run a single test class
./mvnw test -pl :graylog2-server -Dtest=MessageTest -Dskip.web.build=true -Dmaven.javadoc.skip=true

# Run a single test method
./mvnw test -pl :graylog2-server -Dtest=MessageTest#testMethod -Dskip.web.build=true -Dmaven.javadoc.skip=true
```

## Technology Stack

### Dependency Injection
- **Google Guice** — Use `@Inject` constructor injection; bindings defined in Guice modules

### REST API
- **Jersey (JAX-RS)** — Use `@Path`, `@GET`, `@POST`, `@Produces` annotations on resource classes

### Serialization
- **Jackson** — Automatic JSON serialization for REST payloads

### Database
- **MongoDB** with **MongoJack** — Document storage via `MongoCollection<T>` interface

### Search Backend
- **Elasticsearch/OpenSearch** — Pluggable storage adapters (`graylog-storage-elasticsearch7`, `graylog-storage-opensearch2`, `graylog-storage-opensearch3`)

### Testing
- **JUnit 5** + **Mockito** + **AssertJ** — Standard test stack
- **TestContainers** — Integration tests with containerized dependencies

### Key Libraries
- **Guava** — Collections, caching, utilities
- **Apache Shiro** — Authentication/authorization
- **Caffeine** — In-memory caching
- **OkHttp/Retrofit** — HTTP clients

## Development Conventions

### Java

- Java 21 required

#### Code Style

The project uses a customized IntelliJ IDEA Java code style.

**How to apply code style (in order of precedence):**
1. Follow the explicit rules documented below
2. Match the style of surrounding code in the file you are editing
3. If working from the `graylog-project-internal` meta-project, read `.idea/codeStyles/Project.xml` and apply the configured Java code style for anything not covered above

**Key rules:**
- Never use wildcard imports (`import foo.*`)
- Prefer `final` for local variables
- Always add `import` statements (regular or static) instead of using inline fully-qualified class names. Only use fully-qualified names to resolve naming conflicts.
- Always verify import order before completing any Java edits

**Import Order:**
1. All non-static imports except `javax.*`/`java.*`, sorted alphabetically (includes `jakarta.*`)
2. Blank line
3. `javax.*` and `java.*` imports, sorted alphabetically (no blank line between them)
4. Blank line
5. Static imports, sorted alphabetically

**Removing unused imports:**
- Always remove unused imports from files you modify

**String Formatting:**
- For formatted strings (except logging), use the `f()` helper instead of string concatenation
- Import: `import static org.graylog2.shared.utilities.StringUtils.f;`
- Example: `f("Message size exceeds %d bytes", maxSize)` instead of `"Message size exceeds " + maxSize + " bytes"`
- For logging, continue using SLF4J's `{}` placeholders: `LOG.info("Size: {}", size)`

**License Headers:**
- Java files in `src/main/java` and `src/test/java` must have the SSPL license header
- Run `./mvnw license:format` to add missing headers

#### Forbidden APIs

The build enforces API usage rules via the `forbiddenapis` Maven plugin. Certain Java APIs are prohibited — including deprecated methods, APIs that use platform-dependent defaults, and other error-prone patterns. **Before writing new code, read the forbidden API rules** in `pom.xml` (search for `forbiddenapis` in the plugin configuration) so you can avoid violations upfront rather than discovering them at compile time.

Common example: `"text".getBytes()` is forbidden — use `"text".getBytes(StandardCharsets.UTF_8)` instead.

## Feature-Area Guidance

Before working on a specific feature area, check for an `AGENTS.md` in the relevant package or directory. These contain domain-specific context and conventions.

## Before Completing Work

Before considering work complete, review your changes against the conventions in this file.

## PR and Issue Output Guidelines

- When outputting text for PRs, issues, or other GitHub content, always wrap code and file names in backticks and output raw markdown so it can be copied directly.
- Keep PR descriptions brief — focus on motivation and approach, not exhaustive code changes since reviewers can read the diff.
