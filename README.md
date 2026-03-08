# openapi-generator-maven-plugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring-6DB33F?logo=spring&logoColor=white)](https://spring.io)
[![OpenAPI 3.0](https://img.shields.io/badge/OpenAPI-3.0-green?logo=openapiinitiative)](https://swagger.io/specification/)
![REST API](https://img.shields.io/badge/REST-API-blue)

Maven plugin that generates an OpenAPI 3.0 specification from compiled Spring MVC controllers — **no running server required**.

The plugin binds to the `process-classes` Maven lifecycle phase and scans your compiled classes via reflection, walking the full type hierarchy of every controller to collect Spring MVC routing annotations and SpringDoc documentation annotations.

---

## Table of Contents

- [How It Works](#how-it-works)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Minimal Configuration](#minimal-configuration)
- [Configuration Reference](#configuration-reference)
  - [Required Parameters](#required-parameters)
  - [Optional Parameters](#optional-parameters)
  - [Servers](#servers)
  - [Security Schemes](#security-schemes)
  - [Custom Controller Annotations](#custom-controller-annotations)
- [Skipping the Goal](#skipping-the-goal)
- [Output Formats](#output-formats)
- [License](#license)

---

## How It Works

1. After the compiler produces `.class` files, the plugin builds a `URLClassLoader` from the project's compile and runtime classpath.
2. It scans every class in the configured `basePackages` for controller stereotypes (`@RestController`, `@Controller`, or any custom annotation configured via `controllerAnnotations`), using recursive meta-annotation traversal.
3. For each controller, it walks the full type hierarchy — superclasses and interfaces — collecting routing annotations (`@GetMapping`, `@PostMapping`, etc.) and SpringDoc annotations (`@Operation`, `@ApiResponse`, `@Tag`, etc.).
4. It resolves DTO types into OpenAPI schemas via reflection and assembles the final `OpenAPI` object.
5. The result is serialised to YAML (or JSON) and written to the configured `outputFile`.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| `openapi-generator-parent` | latest (must be installed locally) |

---

## Installation

Add the plugin to your project's `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.rspereiratech</groupId>
      <artifactId>openapi-generator-maven-plugin</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <goals>
            <goal>generate</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <basePackages>
          <basePackage>com.example.controller</basePackage>
        </basePackages>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Then run:

```bash
mvn process-classes
```

---

## Minimal Configuration

```xml
<configuration>
  <basePackages>
    <basePackage>com.example.controller</basePackage>
  </basePackages>
  <title>My API</title>
  <version>1.0.0</version>
</configuration>
```

---

## Configuration Reference

### Required Parameters

| Parameter | Description |
|---|---|
| `basePackages` | List of base packages to scan for Spring MVC controllers. At least one entry is required. |

### Optional Parameters

| Parameter | Default | Description |
|---|---|---|
| `outputFile` | `docs/swagger/openapi.yaml` | Path where the generated file is written. |
| `title` | `API` | API title written into the `info` block. |
| `description` | _(empty)_ | API description written into the `info` block. |
| `version` | `1.0.0` | API version written into the `info` block. |
| `contactName` | — | Contact name written into `info.contact`. |
| `contactEmail` | — | Contact email written into `info.contact`. |
| `contactUrl` | — | Contact URL written into `info.contact`. |
| `licenseName` | — | License name written into `info.license`. |
| `licenseUrl` | — | License URL written into `info.license`. |
| `contextPath` | — | Appended to every server URL as a path segment. |
| `outputFormat` | `YAML` | Output format: `YAML` or `JSON`. |
| `skip` | `false` | Skips goal execution when `true`. |

### Servers

Use `<servers>` to define multiple server environments. When provided, it takes precedence over the `<serverUrl>` shorthand.

```xml
<servers>
  <server>
    <url>https://api.example.com</url>
    <description>Production</description>
  </server>
  <server>
    <url>https://staging.example.com</url>
    <description>Staging</description>
  </server>
  <server>
    <url>http://localhost:8080</url>
    <description>Local development</description>
  </server>
</servers>
```

For a single server, use the shorthand (ignored when `<servers>` is present):

```xml
<serverUrl>https://api.example.com</serverUrl>
```

### Security Schemes

Supported types: `http` (Bearer, Basic), `apiKey`, `openIdConnect`.

**Bearer JWT:**

```xml
<securitySchemes>
  <securityScheme>
    <name>bearerAuth</name>
    <type>http</type>
    <scheme>bearer</scheme>
    <bearerFormat>JWT</bearerFormat>
    <description>JWT token obtained from the auth service</description>
  </securityScheme>
</securitySchemes>
```

**API Key in header:**

```xml
<securityScheme>
  <name>apiKeyAuth</name>
  <type>apiKey</type>
  <in>header</in>
  <parameterName>X-API-Key</parameterName>
</securityScheme>
```

Each security scheme is automatically added to both `components/securitySchemes` and the root `security` list.

### Custom Controller Annotations

By default the plugin detects classes annotated with `@RestController` or `@Controller`, including through recursive meta-annotation traversal (so composed annotations like `@CustomRestController` are detected automatically).

Use `<controllerAnnotations>` only when your custom annotation does **not** transitively meta-annotate a Spring controller stereotype:

```xml
<controllerAnnotations>
  <controllerAnnotation>com.example.annotation.MyApiEndpoint</controllerAnnotation>
</controllerAnnotations>
```

---

## Skipping the Goal

Via configuration:

```xml
<skip>true</skip>
```

Via command line:

```bash
mvn process-classes -Dopenapi.generator.skip=true
```

---

## Output Formats

The plugin supports `YAML` (default) and `JSON` output.

Via configuration:

```xml
<outputFormat>JSON</outputFormat>
```

Via command line:

```bash
mvn process-classes -Dopenapi.generator.outputFormat=JSON
```

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
