# Configuration Reference

Full reference for all parameters accepted by the `generate` goal.

---

## Required Parameters

### `basePackages`

List of base packages to scan for Spring MVC controllers. At least one non-blank entry is required.

```xml
<basePackages>
  <basePackage>com.example.controller</basePackage>
  <basePackage>com.example.api</basePackage>
</basePackages>
```

The scanner finds all classes in these packages (and their subpackages) that are annotated with `@RestController`, `@Controller`, or any annotation listed in `controllerAnnotations`.

---

## Info Block

### `title`

API title written into the `info.title` field.

```xml
<title>My REST API</title>
```

Default: `API`

---

### `description`

API description written into `info.description`.

```xml
<description>Manages orders and products for the e-commerce platform.</description>
```

Default: _(empty)_

---

### `version`

API version written into `info.version`.

```xml
<version>2.1.0</version>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.version=2.1.0
```

Default: `1.0.0`

---

### `contactName`, `contactEmail`, `contactUrl`

Contact information written into `info.contact`.

```xml
<contactName>API Team</contactName>
<contactEmail>api@example.com</contactEmail>
<contactUrl>https://developer.example.com</contactUrl>
```

All three are optional. Omit the entire block if not needed.

---

### `licenseName`, `licenseUrl`

License information written into `info.license`.

```xml
<licenseName>Apache 2.0</licenseName>
<licenseUrl>https://www.apache.org/licenses/LICENSE-2.0</licenseUrl>
```

---

## Output

### `outputFile`

Path of the generated file, relative to the project base directory.

```xml
<outputFile>docs/swagger/openapi.yaml</outputFile>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.outputFile=target/my-api.yaml
```

Default: `docs/swagger/openapi.yaml`

The file is always overwritten on each build. Parent directories are created automatically if they do not exist.

---

### `outputFormat`

Controls whether the output is YAML or JSON. Case-insensitive.

```xml
<outputFormat>JSON</outputFormat>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.outputFormat=JSON
```

Accepted values: `YAML` (default), `JSON`

---

## Servers

### `servers`

List of server environments. Each entry accepts `url` (required) and `description` (optional).

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

When `<servers>` is present, `<serverUrl>` is ignored.

---

### `serverUrl`

Shorthand for a single server URL. Ignored when `<servers>` is provided.

```xml
<serverUrl>https://api.example.com</serverUrl>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.serverUrl=https://api.example.com
```

Default: `/`

---

### `contextPath`

Appended to every server URL as a path segment. Useful for servlet context paths.

```xml
<contextPath>my-api</contextPath>
```

Given `https://api.example.com` and `contextPath=my-api`, the server URL in the generated spec becomes `https://api.example.com/my-api/`.

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.contextPath=my-api
```

Omit or leave blank to use server URLs as-is.

---

## Security Schemes

### `securitySchemes`

List of security schemes added to `components/securitySchemes`. Each scheme is also automatically added to the root `security` list.

#### Bearer JWT

```xml
<securitySchemes>
  <securityScheme>
    <name>bearerAuth</name>
    <type>http</type>
    <scheme>bearer</scheme>
    <bearerFormat>JWT</bearerFormat>
    <description>JWT token obtained from the auth service (Authorization: Bearer &lt;token&gt;)</description>
  </securityScheme>
</securitySchemes>
```

#### API Key in header

```xml
<securityScheme>
  <name>apiKeyAuth</name>
  <type>apiKey</type>
  <in>header</in>
  <parameterName>X-API-Key</parameterName>
</securityScheme>
```

#### OpenID Connect

```xml
<securityScheme>
  <name>openIdConnect</name>
  <type>openIdConnect</type>
  <openIdConnectUrl>https://example.com/.well-known/openid-configuration</openIdConnectUrl>
</securityScheme>
```

#### Multiple schemes

```xml
<securitySchemes>
  <securityScheme>
    <name>bearerAuth</name>
    <type>http</type>
    <scheme>bearer</scheme>
    <bearerFormat>JWT</bearerFormat>
  </securityScheme>
  <securityScheme>
    <name>apiKeyAuth</name>
    <type>apiKey</type>
    <in>header</in>
    <parameterName>X-API-Key</parameterName>
  </securityScheme>
</securitySchemes>
```

`name` and `type` are required. All other fields are optional and depend on the scheme type.

---

## Controller Detection

### `controllerAnnotations`

Additional annotation class names treated as controller stereotypes, beyond the built-in `@RestController` and `@Controller`.

```xml
<controllerAnnotations>
  <controllerAnnotation>com.example.annotation.MyApiEndpoint</controllerAnnotation>
</controllerAnnotations>
```

**Note:** This is only needed for annotations that do **not** transitively meta-annotate `@RestController` or `@Controller`. Composed annotations like `@CustomRestController` — which are themselves meta-annotated with `@RestController` — are detected automatically without this setting.

---

## Skip

### `sortOutput`

When `true`, enables deterministic output:

- Controllers are sorted alphabetically by canonical class name before processing.
- The `paths` block is sorted alphabetically after all controllers are processed.

This guarantees that the generated spec is byte-for-byte identical across machines and builds, regardless of filesystem or JVM ordering. Useful for version-controlled spec files where noise-free diffs matter.

```xml
<sortOutput>true</sortOutput>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.sortOutput=true
```

Default: `false`

---

## Pagination and Virtual Parameters

`Pageable` handling and the virtual-parameter pattern (`@Parameter` / `@Parameters` at method
level) are implemented entirely in `openapi-generator-core` and require no additional plugin
configuration. See `docs/Architecture.md` in `openapi-generator-core` for the full explanation.

---

## Parameter Type Filtering

### `ignoreDefaultParamTypes`

When `true` (default), the following framework-injected parameter types are silently skipped and never appear as OpenAPI parameters:

`java.util.Locale`, `java.security.Principal`, `jakarta.servlet.http.HttpServletRequest`, `jakarta.servlet.http.HttpServletResponse`, `jakarta.servlet.http.HttpSession`, `jakarta.servlet.ServletRequest`, `jakarta.servlet.ServletResponse`, `org.springframework.web.context.request.WebRequest`, `org.springframework.web.context.request.NativeWebRequest`, `org.springframework.validation.BindingResult`, `org.springframework.validation.Errors`, `org.springframework.ui.Model`, `org.springframework.ui.ModelMap`

Set to `false` to disable this behaviour:

```xml
<ignoreDefaultParamTypes>false</ignoreDefaultParamTypes>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.ignoreDefaultParamTypes=false
```

Default: `true`

---

### `additionalIgnoredParamTypes`

Fully-qualified class names of extra parameter types to ignore, applied on top of the built-in defaults. Useful for project-specific types that should never appear as OpenAPI parameters.

```xml
<additionalIgnoredParamTypes>
  <additionalIgnoredParamType>com.example.security.TenantContext</additionalIgnoredParamType>
  <additionalIgnoredParamType>com.example.audit.AuditContext</additionalIgnoredParamType>
</additionalIgnoredParamTypes>
```

Default: _(empty)_

---

### `defaultProducesMediaType`

The fallback media type used in response `content` blocks when no `produces` attribute is declared on the mapping annotation and no `@Content(mediaType = ...)` is specified.

Mirrors `springdoc.default-produces-media-type`. Explicit `produces` values always take precedence.

```xml
<defaultProducesMediaType>application/json</defaultProducesMediaType>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.defaultProducesMediaType=application/json
```

Default: `*/*`

---

### `defaultConsumesMediaType`

The fallback media type used in request body `content` blocks when no `consumes` attribute is declared on the mapping annotation and no `@Content(mediaType = ...)` is specified.

Mirrors `springdoc.default-consumes-media-type`. Explicit `consumes` values always take precedence.

```xml
<defaultConsumesMediaType>application/xml</defaultConsumesMediaType>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.defaultConsumesMediaType=application/xml
```

Default: `application/json`

---

### `skip`

Skips goal execution entirely when set to `true`.

```xml
<skip>true</skip>
```

Can also be set on the command line:

```bash
mvn process-classes -Dopenapi.generator.skip=true
```

Default: `false`

---

## Complete Example

```xml
<plugin>
  <groupId>io.github.rspereiratech</groupId>
  <artifactId>openapi-generator-maven-plugin</artifactId>
  <version>{latest-version}</version>
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
      <basePackage>com.example.api</basePackage>
    </basePackages>
    <outputFile>docs/swagger/openapi.yaml</outputFile>
    <title>My API</title>
    <description>Production REST API</description>
    <version>1.0.0</version>
    <contactName>API Team</contactName>
    <contactEmail>api@example.com</contactEmail>
    <licenseName>Apache 2.0</licenseName>
    <licenseUrl>https://www.apache.org/licenses/LICENSE-2.0</licenseUrl>
    <contextPath>my-api</contextPath>
    <servers>
      <server>
        <url>https://api.example.com</url>
        <description>Production</description>
      </server>
      <server>
        <url>http://localhost:8080</url>
        <description>Local development</description>
      </server>
    </servers>
    <securitySchemes>
      <securityScheme>
        <name>bearerAuth</name>
        <type>http</type>
        <scheme>bearer</scheme>
        <bearerFormat>JWT</bearerFormat>
      </securityScheme>
    </securitySchemes>
    <controllerAnnotations>
      <controllerAnnotation>com.example.annotation.MyApiEndpoint</controllerAnnotation>
    </controllerAnnotations>
    <sortOutput>true</sortOutput>
  </configuration>
</plugin>
```
