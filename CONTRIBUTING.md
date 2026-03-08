# Contributing

Thank you for your interest in contributing to `openapi-generator-maven-plugin`.

This module is the Maven plugin layer that bridges the Maven lifecycle with `openapi-generator-core`. Contributions that improve parameter handling, error messaging, classloader behaviour, or test coverage are welcome.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Building Locally](#building-locally)
- [Running the Tests](#running-the-tests)
- [Adding a New Configuration Parameter](#adding-a-new-configuration-parameter)
- [Code Style](#code-style)
- [Submitting a Pull Request](#submitting-a-pull-request)

---

## Prerequisites

- Java 21+
- Maven 3.9+
- `openapi-generator-parent` installed locally

---

## Project Structure

```
openapi-generator-maven-plugin/
├── src/
│   ├── main/java/.../
│   │   └── OpenApiGeneratorMojo.java   # Single Mojo — all plugin logic lives here
│   └── test/java/.../
│       └── OpenApiGeneratorMojoTest.java
└── pom.xml
```

The plugin is intentionally thin. All generation logic lives in `openapi-generator-core`. The Mojo's responsibilities are:

1. Reading Maven configuration parameters.
2. Validating them.
3. Building a `GeneratorConfig` and a `URLClassLoader` from the project classpath.
4. Delegating to `OpenApiGeneratorImpl`.

---

## Building Locally

```bash
# Install the parent POM first
mvn install -f ../openapi-generator-parent/pom.xml

# Build the plugin
mvn install
```

---

## Running the Tests

```bash
mvn test
```

Tests use JUnit 5 and Mockito. Private methods are tested via reflection — see the helper utilities at the top of `OpenApiGeneratorMojoTest`.

---

## Adding a New Configuration Parameter

1. **Declare the field** in `OpenApiGeneratorMojo` with a `@Parameter` annotation:

```java
/** Description of the new parameter. */
@Parameter(defaultValue = "someDefault")
private String myNewParam;
```

2. **Wire it into `buildConfig()`** — pass the value to the `GeneratorConfig.Builder`:

```java
builder.myNewParam(myNewParam);
```

3. **Add validation** in `validateParameters()` if the parameter is required or has constraints.

4. **Document it** in:
   - The field Javadoc in `OpenApiGeneratorMojo`
   - The configuration table in `README.md`
   - `CHANGELOG.md` under the appropriate version

5. **Write tests** covering:
   - The value is correctly propagated to `GeneratorConfig`
   - Edge cases (null, blank, invalid values)

---

## Code Style

- Follow the existing code structure — one Mojo class, inner POJOs for XML-bound entries.
- Use Lombok `@Getter`/`@Setter` on XML-bound POJOs.
- Keep `execute()` thin — delegate logic to private methods (`validateParameters`, `buildConfig`, `buildProjectClassLoader`).
- All private methods must have Javadoc.
- Tests should use `@DisplayName` with descriptive names and `@Nested` classes grouped by method under test.

---

## Submitting a Pull Request

1. Fork the repository and create a branch from `master`.
2. Implement your changes following the guidelines above.
3. Ensure `mvn test` passes.
4. Update `README.md` and `CHANGELOG.md`.
5. Open a pull request with a clear description of what was changed and why.
