# Plugin Flowchart

This page describes the complete execution flow of `OpenApiGeneratorMojo.execute()`.

---

## Full Flow

```
mvn process-classes
        │
        ▼
┌───────────────────────────┐
│   OpenApiGeneratorMojo    │
│      execute()            │
└─────────────┬─────────────┘
              │
              ▼
      skip == true?
      │         │
     YES        NO
      │         │
      ▼         ▼
   log +   validateParameters()
  return        │
           ┌────┴─────────────────────────────┐
           │                                  │
           │  basePackages null/empty?  ──YES──► MojoExecutionException
           │  any entry blank?          ──YES──► MojoExecutionException
           │                                  │
           └────┬─────────────────────────────┘
                │ OK
                ▼
           buildConfig()
                │
           ┌────┴──────────────────────────────────────┐
           │                                           │
           │  outputFormat invalid?  ──YES──► MojoExecutionException
           │                                           │
           │  <servers> present?                       │
           │    YES ──► add each ServerEntry           │
           │              url blank? ──YES──► MojoExecutionException
           │    NO  ──► use <serverUrl>                │
           │                                           │
           │  <securitySchemes> present?               │
           │    YES ──► add each SecuritySchemeEntry   │
           │              name blank? ──YES──► MojoExecutionException
           │              type blank? ──YES──► MojoExecutionException
           │                                           │
           │  <controllerAnnotations> present?         │
           │    YES ──► add to config                  │
           │                                           │
           └────┬──────────────────────────────────────┘
                │ GeneratorConfig ready
                ▼
        buildProjectClassLoader()
                │
           ┌────┴──────────────────────────────────────┐
           │                                           │
           │  getCompileClasspathElements()            │
           │  + getRuntimeClasspathElements()          │
           │  (deduped)                                │
           │                                           │
           │  classpath unresolvable? ──YES──► MojoExecutionException
           │                                           │
           └────┬──────────────────────────────────────┘
                │ URLClassLoader ready
                ▼
   set thread context classloader
   (projectClassLoader)
                │
                ▼
   OpenApiGeneratorImpl.generate(config, classLoader)
                │
           ┌────┴──────────────────────────────────────┐
           │  (delegates to openapi-generator-core)    │
           │                                           │
           │  exception thrown? ──YES──► MojoExecutionException
           │                                           │
           └────┬──────────────────────────────────────┘
                │ OK
                ▼
   restore original classloader  (finally block)
                │
                ▼
   log "OpenAPI spec written to: <outputFile>"
                │
                ▼
              END
```

---

## Config Assembly Detail

```
buildConfig()
      │
      ├─ basePackages        ──────────────────────► GeneratorConfig.basePackages
      ├─ outputFile          ──────────────────────► GeneratorConfig.outputFile
      ├─ title               ──────────────────────► GeneratorConfig.title
      ├─ description         ──────────────────────► GeneratorConfig.description
      ├─ version             ──────────────────────► GeneratorConfig.version
      ├─ contactName/Email/Url ────────────────────► GeneratorConfig.contact*
      ├─ licenseName/Url     ──────────────────────► GeneratorConfig.license*
      ├─ contextPath         ──────────────────────► GeneratorConfig.contextPath
      ├─ outputFormat        ──────────────────────► GeneratorConfig.outputFormat
      │
      ├─ servers != null && !empty?
      │    YES ──► each ServerEntry ──► ServerConfig.of(url, description)
      │    NO  ──► serverUrl        ──► ServerConfig.of(serverUrl, null)
      │
      ├─ securitySchemes != null && !empty?
      │    YES ──► each SecuritySchemeEntry ──► SecuritySchemeConfig(...)
      │
      └─ controllerAnnotations != null && !empty?
           YES ──► List<String> ──────────────────► GeneratorConfig.controllerAnnotations
```
