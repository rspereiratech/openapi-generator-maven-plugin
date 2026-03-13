# Troubleshooting

Common errors and how to resolve them.

---

## `<basePackages> must contain at least one <basePackage> entry`

**Cause:** The `<basePackages>` block is missing or empty in the plugin configuration.

**Fix:** Add at least one `<basePackage>` entry:

```xml
<basePackages>
  <basePackage>com.example.controller</basePackage>
</basePackages>
```

---

## `<basePackage> values must not be blank`

**Cause:** One or more `<basePackage>` entries contain only whitespace.

**Fix:** Ensure all entries are valid package names:

```xml
<!-- Wrong -->
<basePackage>   </basePackage>

<!-- Correct -->
<basePackage>com.example.controller</basePackage>
```

---

## `Each <server> entry must have a non-blank <url>`

**Cause:** A `<server>` entry in the `<servers>` block is missing the `<url>` element or has a blank value.

**Fix:**

```xml
<!-- Wrong -->
<server>
  <description>Production</description>
</server>

<!-- Correct -->
<server>
  <url>https://api.example.com</url>
  <description>Production</description>
</server>
```

---

## `Each <securityScheme> entry must have a non-blank <name>`

**Cause:** A `<securityScheme>` entry is missing the `<name>` element.

**Fix:**

```xml
<securityScheme>
  <name>bearerAuth</name>
  <type>http</type>
  <scheme>bearer</scheme>
  <bearerFormat>JWT</bearerFormat>
</securityScheme>
```

---

## `Each <securityScheme> entry must have a non-blank <type>`

**Cause:** A `<securityScheme>` entry is missing the `<type>` element.

**Fix:** Provide a valid type — `http`, `apiKey`, or `openIdConnect`:

```xml
<securityScheme>
  <name>bearerAuth</name>
  <type>http</type>
</securityScheme>
```

---

## `<outputFormat> must be YAML or JSON`

**Cause:** The `<outputFormat>` parameter contains an unrecognised value.

**Fix:** Use one of the accepted values (case-insensitive):

```xml
<outputFormat>YAML</outputFormat>
<!-- or -->
<outputFormat>JSON</outputFormat>
```

---

## `Could not resolve project classpath`

**Cause:** Maven could not resolve the compile or runtime classpath dependencies before the plugin ran.

**Fix:** Ensure all dependencies are available in the local repository. Run `mvn dependency:resolve` first, or check for missing or unresolvable dependencies in the build output.

---

## `OpenAPI generation failed`

**Cause:** An exception was thrown inside `openapi-generator-core` during the generation pipeline.

**Fix:** Check the full stack trace in the Maven build output. Common root causes:
- A controller class references a type that is not on the classpath.
- A DTO has a circular reference that cannot be resolved.
- An annotation value is malformed.

Enable Maven debug output for more detail:

```bash
mvn process-classes -X
```

---

## No controllers found — generated spec has no paths

**Cause:** The scanner found no controller classes in the configured `basePackages`.

**Possible reasons and fixes:**

1. **Wrong package name** — verify the package matches the actual package declaration in your source files.

2. **Custom annotation not detected** — if your controllers use a custom stereotype annotation that does not transitively meta-annotate `@RestController`, add it to `<controllerAnnotations>`:

```xml
<controllerAnnotations>
  <controllerAnnotation>com.example.annotation.MyApiEndpoint</controllerAnnotation>
</controllerAnnotations>
```

3. **Classes not compiled** — ensure `mvn compile` has run before `process-classes`, or run `mvn process-classes` directly (which includes the compile phase).

---

## Pagination parameters (`page`, `size`, `sort`) missing from the spec

**Cause:** `Pageable` is mapped to a single `$ref: Pageable` query parameter by default. To emit
individual `page`, `size`, and `sort` parameters instead, use the virtual-parameter pattern.

**Fix:** See `docs/Architecture.md` in `openapi-generator-core` for the full explanation and
code example.

---

## Generated YAML is outdated after renaming or deleting an endpoint

**Cause:** The output file is always overwritten on each build, but the build has not been re-run since the change.

**Fix:** Run `mvn process-classes` again. The plugin does not perform incremental generation — it always produces a full rewrite of the output file.
