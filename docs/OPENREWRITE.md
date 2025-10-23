# OpenRewrite for Graylog

OpenRewrite is configured in `graylog-parent` (`graylog2-server/pom.xml`) and inherited by all modules in both `graylog2-server` and `graylog-plugin-enterprise` repositories.

## Quick Start

Note: `com.example.MyCustomRecipe` is an example placeholder, that refers to the `rewrite.yml.exmaple` recipe. It's good practice to combine
multiple recipes in the `rewrite.yml` at the top of the repository, because that ensures the parse trees are shared, which is more efficient.

### From graylog2-server
```bash
cd /path/to/graylog2-server

# Dry run - see what would change
./mvnw -Popenrewrite rewrite:dryRun \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe

# Review changes in target/rewrite/rewrite.patch

# Apply changes
./mvnw -Popenrewrite rewrite:run \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe
```

### From graylog-plugin-enterprise
```bash
cd /path/to/graylog-plugin-enterprise

# Use mvnw from graylog2-server (inherits parent POM config)
../graylog2-server/mvnw -Popenrewrite rewrite:dryRun \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe
```

## Configuration

- **Plugin Configuration:** `graylog2-server/pom.xml` in `<pluginManagement>` (lines ~739-782)
- **Profile:** `graylog2-server/pom.xml` in `<profiles>` (lines ~1343-1353)
- **Recipe Definitions:** `rewrite.yml` in repository root
- **Recipe Dependencies:** Pre-loaded in parent POM (rewrite-java, rewrite-joda, rewrite-testing-frameworks, rewrite-migrate-java, rewrite-static-analysis)

All modules in both repositories inherit this configuration automatically.

## Common Recipes

All recipe dependencies are pre-configured. Just specify the recipe name:

| Task | Command |
|------|---------|
| **Joda-Time → java.time** | `./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.joda.time.NoJodaTime` |
| **JUnit 4 → 5** | `./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.testing.junit5.JUnit4to5Migration` |
| **Remove unused imports** | `./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.RemoveUnusedImports` |
| **Static analysis fixes** | `./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=org.openrewrite.staticanalysis.CommonStaticAnalysis` |
| **Custom recipes** | `./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=com.example.MyCustomRecipe` |

## Usage Patterns

### Discover Available Recipes
```bash
./mvnw -Popenrewrite rewrite:discover | grep -i <search-term>
```

### Run on Specific Modules
```bash
# Single module
./mvnw -Popenrewrite rewrite:run \
  -pl graylog2-server \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe

# Multiple modules
./mvnw -Popenrewrite rewrite:run \
  -pl graylog2-server,full-backend-tests \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe
```

### Exclude Problematic Files
```bash
./mvnw -Popenrewrite rewrite:run \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe \
  -Drewrite.exclusions='**/ProblemFile.java,**/AnotherFile.java'
```

### Multiple Recipes
```bash
./mvnw -Popenrewrite rewrite:run \
  -Drewrite.activeRecipes=org.openrewrite.java.RemoveUnusedImports,org.openrewrite.java.format.AutoFormat
```

## IntelliJ Integration

### Maven Tool Window
1. Open Maven tool window (View → Tool Windows → Maven)
2. Expand Profiles → Check `openrewrite`
3. Navigate to Plugins → rewrite-maven-plugin → rewrite:dryRun
4. Right-click and select "Run"

### Run Configurations
1. Run → Edit Configurations → Add → Maven
2. Name: "OpenRewrite - Dry Run"
3. Command line: `-Popenrewrite rewrite:dryRun -Drewrite.activeRecipes=com.example.MyCustomRecipe`
4. Working directory: `$ProjectFileDir$`
5. Profiles: `openrewrite` (checked)

Create separate configurations for different recipes or actions (dryRun vs run).

## Custom Recipes

Edit `rewrite.yml` in the repository root to compose recipes:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.graylog.CustomRefactoring
displayName: Custom Graylog Refactoring
description: Project-specific refactoring combining multiple steps
recipeList:
  - org.openrewrite.java.RemoveUnusedImports
  - org.openrewrite.java.format.AutoFormat
  - org.openrewrite.java.joda.time.NoJodaTime
```

For Java-based custom recipes, create a separate Maven module and add it as a dependency in the parent POM's OpenRewrite plugin configuration.

## Troubleshooting

### Recipe Not Found
Ensure you're using the `-Popenrewrite` profile and the correct recipe name (check with `rewrite:discover`).

### Out of Memory
```bash
export MAVEN_OPTS="-Xmx4g"
./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=...
```

### Compilation Errors
OpenRewrite requires the project to compile first:
```bash
./mvnw compile
./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=...
```

### Groovy Parsing Errors
Groovy files (Jenkins pipelines) are automatically excluded via `plainTextMasks` in the parent POM. If you still see errors, add:
```bash
-Drewrite.exclusions='**/*.groovy'
```

### Recipe Crashes on Specific Files
Add exclusions for problematic files and report bugs to OpenRewrite:
```bash
./mvnw -Popenrewrite rewrite:run \
  -Drewrite.activeRecipes=com.example.MyCustomRecipe \
  -Drewrite.exclusions='**/ProblemFile.java'
```

## Best Practices

1. **Always start with dry run** and review `target/rewrite/rewrite.patch`
2. **Use version control** - create a branch before running transformations
3. **Work incrementally** - apply one recipe at a time for complex refactorings
4. **Commit after each recipe** - makes it easier to review and rollback if needed

Example workflow:
```bash
git checkout -b feature/joda-time-migration
./mvnw -Popenrewrite rewrite:dryRun -Drewrite.activeRecipes=org.openrewrite.java.joda.time.NoJodaTime
# Review changes in target/rewrite/rewrite.patch
./mvnw -Popenrewrite rewrite:run -Drewrite.activeRecipes=org.openrewrite.java.joda.time.NoJodaTime
git diff  # Review changes
git commit -am "Migrate from Joda-Time to java.time"
```

## Architecture Note

Configuration is in `graylog-parent` (`graylog2-server/pom.xml`) because:
- It's the actual parent POM for all modules in both repositories
- Provides single source of truth for versions and configuration
- Proper Maven inheritance via `<pluginManagement>` propagates to all child modules
- Supports cross-repository usage (graylog2-server and graylog-plugin-enterprise)

The `graylog-project-internal` POM is just an aggregator for coordinating builds, not a parent.

## Resources

- OpenRewrite Documentation: https://docs.openrewrite.org
- Recipe Catalog: https://docs.openrewrite.org/recipes
- Maven Plugin Reference: https://docs.openrewrite.org/reference/rewrite-maven-plugin
- GitHub: https://github.com/openrewrite/rewrite-maven-plugin
