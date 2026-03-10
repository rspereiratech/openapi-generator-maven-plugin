# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- `sortOutput` parameter — when `true`, controllers are sorted alphabetically by canonical class name before processing and the resulting `paths` block is sorted alphabetically, guaranteeing deterministic spec output across machines and builds; default `false`; configurable via `-Dopenapi.generator.sortOutput`
- `ignoreDefaultParamTypes` parameter — when `true` (default), the built-in list of framework-injected parameter types is silently skipped; set to `false` to disable; configurable via `-Dopenapi.generator.ignoreDefaultParamTypes`
- `additionalIgnoredParamTypes` parameter — list of extra fully-qualified class names to ignore on top of the built-in defaults
- `defaultProducesMediaType` parameter — fallback media type for response bodies when no `produces` attribute is declared; default `*/*`; mirrors `springdoc.default-produces-media-type`; configurable via `-Dopenapi.generator.defaultProducesMediaType`
- `defaultConsumesMediaType` parameter — fallback media type for request bodies when no `consumes` attribute is declared; default `application/json`; mirrors `springdoc.default-consumes-media-type`; configurable via `-Dopenapi.generator.defaultConsumesMediaType`

### Changed

- Upgraded dependency on `openapi-generator-core` to `1.1.0-SNAPSHOT`

---

## [1.0.0] — 2026-03-08

### Added

- `OpenApiGeneratorMojo` — Maven goal `generate` bound to the `process-classes` lifecycle phase
- `basePackages` parameter — required list of packages to scan for Spring MVC controllers
- `outputFile` parameter — path of the generated file (default: `docs/swagger/openapi.yaml`)
- `title`, `description`, `version` parameters — written into the OpenAPI `info` block
- `servers` parameter — list of server entries (`url`, `description`) for the `servers` block
- `serverUrl` parameter — shorthand for a single server URL; ignored when `servers` is present
- `contextPath` parameter — appended to every server URL as a path segment
- `contactName`, `contactEmail`, `contactUrl` parameters — written into `info.contact`
- `licenseName`, `licenseUrl` parameters — written into `info.license`
- `controllerAnnotations` parameter — additional annotation class names treated as controller stereotypes, beyond the built-in `@RestController` and `@Controller`
- `securitySchemes` parameter — security scheme entries written into `components/securitySchemes` and the root `security` list; supports `http` (Bearer, Basic), `apiKey`, and `openIdConnect` types
- `outputFormat` parameter — controls whether the output is `YAML` (default) or `JSON`; configurable via `-Dopenapi.generator.outputFormat`
- `skip` parameter — skips goal execution when set to `true`; configurable via `-Dopenapi.generator.skip`
- `ServerEntry` and `SecuritySchemeEntry` inner POJOs for Maven XML binding
- Project classloader built from compile and runtime classpath elements, set as thread context classloader before delegating to `openapi-generator-core`
