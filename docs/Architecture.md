# Architecture

This page describes the internal mechanics of the plugin — how it integrates with the Maven lifecycle, how it loads project classes, and how it delegates to `openapi-generator-core`.

---

## Lifecycle Binding

The plugin exposes a single goal: `generate`. It is bound by default to the `process-classes` phase:

```
validate → initialize → generate-sources → process-sources → generate-resources
→ process-resources → compile → process-classes ← plugin runs here
→ generate-test-sources → ... → package → ...
```

This phase is chosen because:
- The Java compiler has already finished — `.class` files are available in `target/classes`.
- No JAR has been assembled yet — the spec is produced before packaging, which is useful in multi-module builds where other modules may depend on the generated YAML.

---

## Execution Flow

```
Maven invokes execute()
        │
        ▼
1. skip check          →  if skip=true, return immediately
        │
        ▼
2. validateParameters  →  ensure basePackages is non-empty and non-blank
        │
        ▼
3. buildConfig         →  assemble GeneratorConfig from Mojo parameters
        │
        ▼
4. buildProjectClassLoader  →  URLClassLoader from compile + runtime classpath
        │
        ▼
5. set thread context classloader  →  required for Jackson ModelConverters
        │
        ▼
6. OpenApiGeneratorImpl.generate(config, classLoader)
        │
        ▼
7. restore original classloader (finally block)
```

---

## Classloader Strategy

The Maven plugin runs inside Maven's own classloader, which cannot see the project's compiled classes. To solve this, the Mojo builds a `URLClassLoader` containing:

- All entries from `project.getCompileClasspathElements()`
- All entries from `project.getRuntimeClasspathElements()` (deduped)

This classloader is set as the **thread context classloader** before calling the generator. This is necessary because Jackson's `ModelConverters` — used internally by the core to resolve DTO schemas — discover classes via the thread context classloader through SPI.

The original classloader is always restored in a `finally` block, regardless of outcome.

---

## Configuration Assembly (`buildConfig`)

`buildConfig()` maps each Mojo parameter to the corresponding field of `GeneratorConfig` via its builder. Key rules:

- `<servers>` takes precedence over `<serverUrl>` when both are present.
- `<controllerAnnotations>` and `<securitySchemes>` are only added to the config when non-null and non-empty.
- `outputFormat` is parsed case-insensitively (`YAML` or `JSON`); an invalid value throws `MojoExecutionException` immediately.
- `contextPath` is passed as-is and may be `null`; the core handles the null case by using server URLs unchanged.

---

## XML Binding POJOs

Maven binds `<configuration>` block child elements to Java objects via reflection and setter injection. Two inner POJOs handle this:

| POJO | XML element | Required fields |
|---|---|---|
| `ServerEntry` | `<server>` | `url` |
| `SecuritySchemeEntry` | `<securityScheme>` | `name`, `type` |

Both use Lombok `@Getter`/`@Setter`. Validation is performed in `buildConfig()` before passing values to the core.

---

## Relationship with `openapi-generator-core`

The plugin is a thin orchestration layer. It does not contain any annotation scanning, schema resolution, or YAML serialisation logic. All of that lives in `openapi-generator-core`, which the plugin invokes via:

```java
new OpenApiGeneratorImpl().generate(config, projectClassLoader);
```

The plugin's sole responsibilities are:
- Reading and validating Maven configuration.
- Constructing the `GeneratorConfig`.
- Providing the right classloader.
- Translating exceptions into `MojoExecutionException`.
