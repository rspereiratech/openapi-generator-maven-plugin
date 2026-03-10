/*
 *   ___                   _   ___ ___
 *  / _ \ _ __  ___ _ _   /_\ | _ \_ _|
 * | (_) | '_ \/ -_) ' \ / _ \|  _/| |
 *  \___/| .__/\___|_||_/_/ \_\_| |___|   Generator
 *       |_|
 *
 * MIT License - Copyright (c) 2026 Rui Pereira
 * See LICENSE in the project root for full license information.
 */
package io.github.rspereiratech.openapi.generator.maven.plugin;

import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;
import io.github.rspereiratech.openapi.generator.core.config.SecuritySchemeConfig;
import io.github.rspereiratech.openapi.generator.core.config.ServerConfig;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class OpenApiGeneratorMojoTest {

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in hierarchy of " + clazz);
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokePrivate(Object target, String methodName) throws Exception {
        Method method = findMethod(target.getClass(), methodName);
        method.setAccessible(true);
        try {
            return (T) method.invoke(target);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MojoExecutionException mee) throw mee;
            if (cause instanceof Exception ex)            throw ex;
            throw new RuntimeException(cause);
        }
    }

    private static Method findMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Method '" + name + "' not found in hierarchy of " + clazz);
    }

    private OpenApiGeneratorMojo minimalMojo() throws Exception {
        OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
        setField(mojo, "basePackages", List.of("com.example.app"));
        setField(mojo, "outputFile", new File("target/docs/swagger/openapi.yaml"));
        setField(mojo, "title", "API");
        setField(mojo, "description", "");
        setField(mojo, "version", "1.0.0");
        setField(mojo, "serverUrl", "/");
        return mojo;
    }

    // ==================================================================
    // 1. ServerEntry POJO
    // ==================================================================

    @Nested
    @DisplayName("ServerEntry POJO")
    class ServerEntryTests {

        @Test
        @DisplayName("getUrl / setUrl round-trip")
        void urlGetterSetter() {
            OpenApiGeneratorMojo.ServerEntry entry = new OpenApiGeneratorMojo.ServerEntry();
            entry.setUrl("https://api.example.com");
            Assertions.assertEquals("https://api.example.com", entry.getUrl());
        }

        @Test
        @DisplayName("getDescription / setDescription round-trip")
        void descriptionGetterSetter() {
            OpenApiGeneratorMojo.ServerEntry entry = new OpenApiGeneratorMojo.ServerEntry();
            entry.setDescription("Production server");
            Assertions.assertEquals("Production server", entry.getDescription());
        }

        @Test
        @DisplayName("default values are null before any setter is called")
        void defaultsAreNull() {
            OpenApiGeneratorMojo.ServerEntry entry = new OpenApiGeneratorMojo.ServerEntry();
            Assertions.assertNull(entry.getUrl());
            Assertions.assertNull(entry.getDescription());
        }

        @Test
        @DisplayName("can set both url and description independently")
        void independentSetters() {
            OpenApiGeneratorMojo.ServerEntry entry = new OpenApiGeneratorMojo.ServerEntry();
            entry.setUrl("https://staging.example.com");
            entry.setDescription("Staging");

            Assertions.assertEquals("https://staging.example.com", entry.getUrl());
            Assertions.assertEquals("Staging", entry.getDescription());
        }
    }

    // ==================================================================
    // 2 & 3. validateParameters via reflection
    // ==================================================================

    @Nested
    @DisplayName("validateParameters (via reflection)")
    class ValidateParametersTests {

        @Test
        @DisplayName("throws MojoExecutionException when basePackages is null")
        void throwsWhenBasePackagesNull() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages", null);

            MojoExecutionException ex = Assertions.assertThrows(
                    MojoExecutionException.class,
                    () -> invokePrivate(mojo, "validateParameters"));

            Assertions.assertTrue(ex.getMessage().contains("basePackages"),
                    "Error message should mention 'basePackages'");
        }

        @Test
        @DisplayName("throws MojoExecutionException when basePackages is empty")
        void throwsWhenBasePackagesEmpty() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages", List.of());

            MojoExecutionException ex = Assertions.assertThrows(
                    MojoExecutionException.class,
                    () -> invokePrivate(mojo, "validateParameters"));

            Assertions.assertTrue(ex.getMessage().contains("basePackages"),
                    "Error message should mention 'basePackages'");
        }

        @Test
        @DisplayName("throws MojoExecutionException when a package entry is blank")
        void throwsWhenBlankPackageEntry() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages", List.of("com.example.app", "   "));

            MojoExecutionException ex = Assertions.assertThrows(
                    MojoExecutionException.class,
                    () -> invokePrivate(mojo, "validateParameters"));

            Assertions.assertTrue(ex.getMessage().contains("blank"),
                    "Error message should mention 'blank'");
        }

        @Test
        @DisplayName("throws MojoExecutionException when a package entry is an empty string")
        void throwsWhenEmptyStringPackageEntry() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages", List.of(""));

            MojoExecutionException ex = Assertions.assertThrows(
                    MojoExecutionException.class,
                    () -> invokePrivate(mojo, "validateParameters"));

            Assertions.assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("does not throw when a single valid package is provided")
        void doesNotThrowWithValidPackage() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages", List.of("com.example.controllers"));

            Assertions.assertDoesNotThrow(() -> invokePrivate(mojo, "validateParameters"));
        }

        @Test
        @DisplayName("does not throw when multiple valid packages are provided")
        void doesNotThrowWithMultipleValidPackages() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages",
                    List.of("com.example.controllers", "com.example.api"));

            Assertions.assertDoesNotThrow(() -> invokePrivate(mojo, "validateParameters"));
        }
    }

    // ==================================================================
    // 4. buildConfig – basic field mapping
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – basic field mapping (via reflection)")
    class BuildConfigBasicTests {

        @Test
        @DisplayName("basePackages are propagated to GeneratorConfig")
        void basePackagesPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "basePackages", List.of("com.example.web", "com.example.api"));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(List.of("com.example.web", "com.example.api"),
                    config.basePackages());
        }

        @Test
        @DisplayName("outputFile absolute path is propagated to GeneratorConfig")
        void outputFilePropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            File outFile = new File("target/my-openapi.yaml");
            setField(mojo, "outputFile", outFile);

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(outFile.getAbsolutePath(), config.outputFile());
        }

        @Test
        @DisplayName("title is propagated to GeneratorConfig")
        void titlePropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "title", "My Awesome API");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("My Awesome API", config.title());
        }

        @Test
        @DisplayName("description is propagated to GeneratorConfig")
        void descriptionPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "description", "A comprehensive REST API");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("A comprehensive REST API", config.description());
        }

        @Test
        @DisplayName("version is propagated to GeneratorConfig")
        void versionPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "version", "3.2.1");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("3.2.1", config.version());
        }

        @Test
        @DisplayName("contactName is propagated to GeneratorConfig")
        void contactNamePropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "contactName", "Alice Smith");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("Alice Smith", config.contactName());
        }

        @Test
        @DisplayName("contactEmail is propagated to GeneratorConfig")
        void contactEmailPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "contactEmail", "alice@example.com");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("alice@example.com", config.contactEmail());
        }

        @Test
        @DisplayName("licenseName is propagated to GeneratorConfig")
        void licenseNamePropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "licenseName", "Apache 2.0");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("Apache 2.0", config.licenseName());
        }

        @Test
        @DisplayName("licenseUrl is propagated to GeneratorConfig")
        void licenseUrlPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "licenseUrl", "https://www.apache.org/licenses/LICENSE-2.0");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("https://www.apache.org/licenses/LICENSE-2.0",
                    config.licenseUrl());
        }

        @Test
        @DisplayName("all standard info fields are correct in a single combined call")
        void allInfoFieldsCombined() throws Exception {
            OpenApiGeneratorMojo mojo = new OpenApiGeneratorMojo();
            setField(mojo, "basePackages", List.of("com.example.app"));
            setField(mojo, "outputFile", new File("target/docs/swagger/openapi.yaml"));
            setField(mojo, "title", "Full API");
            setField(mojo, "description", "Full description");
            setField(mojo, "version", "2.0.0");
            setField(mojo, "serverUrl", "https://api.example.com");
            setField(mojo, "contactName", "Bob Jones");
            setField(mojo, "contactEmail", "bob@example.com");
            setField(mojo, "licenseName", "MIT");
            setField(mojo, "licenseUrl", "https://opensource.org/licenses/MIT");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertAll(
                    () -> Assertions.assertEquals(List.of("com.example.app"), config.basePackages()),
                    () -> Assertions.assertEquals("Full API", config.title()),
                    () -> Assertions.assertEquals("Full description", config.description()),
                    () -> Assertions.assertEquals("2.0.0", config.version()),
                    () -> Assertions.assertEquals("Bob Jones", config.contactName()),
                    () -> Assertions.assertEquals("bob@example.com", config.contactEmail()),
                    () -> Assertions.assertEquals("MIT", config.licenseName()),
                    () -> Assertions.assertEquals("https://opensource.org/licenses/MIT",
                            config.licenseUrl())
            );
        }
    }

    // ==================================================================
    // 5. buildConfig – <servers> block wiring
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – <servers> block wiring (via reflection)")
    class BuildConfigServersTests {

        @Test
        @DisplayName("servers from <servers> list are propagated to GeneratorConfig")
        void serversListPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.ServerEntry prod = new OpenApiGeneratorMojo.ServerEntry();
            prod.setUrl("https://api.example.com");
            prod.setDescription("Production");

            OpenApiGeneratorMojo.ServerEntry staging = new OpenApiGeneratorMojo.ServerEntry();
            staging.setUrl("https://staging.example.com");
            staging.setDescription("Staging");

            setField(mojo, "servers", List.of(prod, staging));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            List<ServerConfig> servers = config.servers();
            Assertions.assertEquals(2, servers.size());

            Assertions.assertEquals("https://api.example.com", servers.get(0).url());
            Assertions.assertEquals("Production",              servers.get(0).description());

            Assertions.assertEquals("https://staging.example.com", servers.get(1).url());
            Assertions.assertEquals("Staging",                     servers.get(1).description());
        }

        @Test
        @DisplayName("a single server entry is correctly mapped")
        void singleServerEntry() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.ServerEntry entry = new OpenApiGeneratorMojo.ServerEntry();
            entry.setUrl("https://api.example.com");
            entry.setDescription("Main");

            setField(mojo, "servers", List.of(entry));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(1, config.servers().size());
            Assertions.assertEquals("https://api.example.com", config.servers().get(0).url());
            Assertions.assertEquals("Main",                    config.servers().get(0).description());
        }

        @Test
        @DisplayName("throws MojoExecutionException when a server entry has a blank url")
        void throwsWhenServerUrlBlank() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.ServerEntry bad = new OpenApiGeneratorMojo.ServerEntry();
            bad.setUrl("   "); // blank
            bad.setDescription("Bad server");

            setField(mojo, "servers", List.of(bad));

            Assertions.assertThrows(MojoExecutionException.class,
                    () -> invokePrivate(mojo, "buildConfig"));
        }

        @Test
        @DisplayName("throws MojoExecutionException when a server entry has a null url")
        void throwsWhenServerUrlNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.ServerEntry bad = new OpenApiGeneratorMojo.ServerEntry();
            // url intentionally left null

            setField(mojo, "servers", List.of(bad));

            Assertions.assertThrows(MojoExecutionException.class,
                    () -> invokePrivate(mojo, "buildConfig"));
        }
    }

    // ==================================================================
    // 6. buildConfig – fallback to serverUrl when servers is null/empty
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – serverUrl fallback (via reflection)")
    class BuildConfigServerUrlFallbackTests {

        @Test
        @DisplayName("serverUrl is used when servers field is null")
        void fallbackToServerUrlWhenServersNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "servers", null);
            setField(mojo, "serverUrl", "https://fallback.example.com");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            List<ServerConfig> servers = config.servers();
            Assertions.assertEquals(1, servers.size());
            Assertions.assertEquals("https://fallback.example.com", servers.get(0).url());
        }

        @Test
        @DisplayName("serverUrl is used when servers field is an empty list")
        void fallbackToServerUrlWhenServersEmpty() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "servers", List.of());
            setField(mojo, "serverUrl", "https://fallback.example.com");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            List<ServerConfig> servers = config.servers();
            Assertions.assertEquals(1, servers.size());
            Assertions.assertEquals("https://fallback.example.com", servers.get(0).url());
        }

        @Test
        @DisplayName("<servers> takes precedence over serverUrl when both are set")
        void serversListTakesPrecedenceOverServerUrl() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.ServerEntry entry = new OpenApiGeneratorMojo.ServerEntry();
            entry.setUrl("https://primary.example.com");
            entry.setDescription("Primary");

            setField(mojo, "servers", List.of(entry));
            setField(mojo, "serverUrl", "https://ignored.example.com");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            List<ServerConfig> servers = config.servers();
            Assertions.assertEquals(1, servers.size());
            Assertions.assertEquals("https://primary.example.com", servers.get(0).url(),
                    "The <servers> entry should take precedence over serverUrl");
        }
    }

    // ==================================================================
    // 7. buildConfig – controllerAnnotations wiring
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – controllerAnnotations wiring (via reflection)")
    class BuildConfigControllerAnnotationsTests {

        @Test
        @DisplayName("controllerAnnotations are propagated to GeneratorConfig")
        void controllerAnnotationsPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "controllerAnnotations",
                    List.of("com.example.annotation.MyApiEndpoint",
                            "com.example.annotation.AnotherEndpoint"));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(
                    List.of("com.example.annotation.MyApiEndpoint",
                            "com.example.annotation.AnotherEndpoint"),
                    config.controllerAnnotations());
        }

        @Test
        @DisplayName("controllerAnnotations is empty in GeneratorConfig when field is null")
        void controllerAnnotationsEmptyWhenNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "controllerAnnotations", null);

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertTrue(config.controllerAnnotations().isEmpty(),
                    "controllerAnnotations should be empty when mojo field is null");
        }

        @Test
        @DisplayName("controllerAnnotations is empty in GeneratorConfig when field is an empty list")
        void controllerAnnotationsEmptyWhenEmptyList() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "controllerAnnotations", List.of());

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertTrue(config.controllerAnnotations().isEmpty(),
                    "controllerAnnotations should be empty when mojo field is an empty list");
        }

        @Test
        @DisplayName("a single controllerAnnotation entry is correctly propagated")
        void singleControllerAnnotation() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "controllerAnnotations",
                    List.of("com.example.annotation.CustomRestController"));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(List.of("com.example.annotation.CustomRestController"),
                    config.controllerAnnotations());
        }
    }

    // ==================================================================
    // 8. SecuritySchemeEntry POJO
    // ==================================================================

    @Nested
    @DisplayName("SecuritySchemeEntry POJO")
    class SecuritySchemeEntryTests {

        @Test
        @DisplayName("all getters and setters round-trip correctly")
        void allGettersAndSettersRoundTrip() {
            OpenApiGeneratorMojo.SecuritySchemeEntry entry = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            entry.setName("bearerAuth");
            entry.setType("http");
            entry.setScheme("bearer");
            entry.setBearerFormat("JWT");
            entry.setDescription("Bearer token authentication");
            entry.setIn("header");
            entry.setParameterName("X-API-Key");
            entry.setOpenIdConnectUrl("https://example.com/.well-known/openid-configuration");

            Assertions.assertAll(
                    () -> Assertions.assertEquals("bearerAuth",    entry.getName()),
                    () -> Assertions.assertEquals("http",          entry.getType()),
                    () -> Assertions.assertEquals("bearer",        entry.getScheme()),
                    () -> Assertions.assertEquals("JWT",           entry.getBearerFormat()),
                    () -> Assertions.assertEquals("Bearer token authentication", entry.getDescription()),
                    () -> Assertions.assertEquals("header",        entry.getIn()),
                    () -> Assertions.assertEquals("X-API-Key",     entry.getParameterName()),
                    () -> Assertions.assertEquals(
                            "https://example.com/.well-known/openid-configuration",
                            entry.getOpenIdConnectUrl())
            );
        }

        @Test
        @DisplayName("all fields default to null before setters are called")
        void defaultsAreNull() {
            OpenApiGeneratorMojo.SecuritySchemeEntry entry = new OpenApiGeneratorMojo.SecuritySchemeEntry();

            Assertions.assertAll(
                    () -> Assertions.assertNull(entry.getName()),
                    () -> Assertions.assertNull(entry.getType()),
                    () -> Assertions.assertNull(entry.getScheme()),
                    () -> Assertions.assertNull(entry.getBearerFormat()),
                    () -> Assertions.assertNull(entry.getDescription()),
                    () -> Assertions.assertNull(entry.getIn()),
                    () -> Assertions.assertNull(entry.getParameterName()),
                    () -> Assertions.assertNull(entry.getOpenIdConnectUrl())
            );
        }
    }

    // ==================================================================
    // 9. buildConfig – ignoreDefaultParamTypes wiring
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – ignoreDefaultParamTypes wiring (via reflection)")
    class BuildConfigIgnoreDefaultParamTypesTests {

        @Test
        @DisplayName("ignoreDefaultParamTypes defaults to true")
        void defaultsToTrue() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertTrue(config.ignoreDefaultParamTypes(),
                    "ignoreDefaultParamTypes must default to true");
        }

        @Test
        @DisplayName("ignoreDefaultParamTypes false is propagated to GeneratorConfig")
        void falseIsPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "ignoreDefaultParamTypes", false);

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertFalse(config.ignoreDefaultParamTypes());
        }

        @Test
        @DisplayName("additionalIgnoredParamTypes are propagated to GeneratorConfig")
        void additionalTypesArePropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "additionalIgnoredParamTypes",
                    List.of("com.example.MyContext", "com.example.OtherContext"));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(
                    List.of("com.example.MyContext", "com.example.OtherContext"),
                    config.additionalIgnoredParamTypes());
        }

        @Test
        @DisplayName("additionalIgnoredParamTypes is empty when mojo field is null")
        void emptyWhenNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "additionalIgnoredParamTypes", null);

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertTrue(config.additionalIgnoredParamTypes().isEmpty());
        }
    }

    // ==================================================================
    // 11. buildConfig – contextPath wiring
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – contextPath wiring (via reflection)")
    class BuildConfigApplicationNameTests {

        @Test
        @DisplayName("contextPath is propagated to GeneratorConfig when set")
        void contextPathPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "contextPath", "vcc-superx-api");

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals("vcc-superx-api", config.contextPath());
        }

        @Test
        @DisplayName("contextPath is null in GeneratorConfig when mojo field is null")
        void contextPathNullWhenNotSet() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "contextPath", null);

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertNull(config.contextPath());
        }
    }

    // ==================================================================
    // 12. buildConfig – <securitySchemes> block wiring
    // ==================================================================

    @Nested
    @DisplayName("buildConfig – <securitySchemes> block wiring (via reflection)")
    class BuildConfigSecuritySchemesTests {

        @Test
        @DisplayName("a single security scheme entry is propagated to GeneratorConfig")
        void singleSchemeEntryPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.SecuritySchemeEntry entry = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            entry.setName("bearerAuth");
            entry.setType("http");
            entry.setScheme("bearer");
            entry.setBearerFormat("JWT");

            setField(mojo, "securitySchemes", List.of(entry));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(1, config.securitySchemes().size());
            SecuritySchemeConfig scheme = config.securitySchemes().get(0);
            Assertions.assertAll(
                    () -> Assertions.assertEquals("bearerAuth", scheme.name()),
                    () -> Assertions.assertEquals("http",       scheme.type()),
                    () -> Assertions.assertEquals("bearer",     scheme.scheme()),
                    () -> Assertions.assertEquals("JWT",        scheme.bearerFormat())
            );
        }

        @Test
        @DisplayName("multiple security schemes are all propagated")
        void multipleSchemesPropagated() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.SecuritySchemeEntry e1 = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            e1.setName("bearerAuth");
            e1.setType("http");

            OpenApiGeneratorMojo.SecuritySchemeEntry e2 = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            e2.setName("apiKeyAuth");
            e2.setType("apiKey");

            setField(mojo, "securitySchemes", List.of(e1, e2));

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertEquals(2, config.securitySchemes().size());
        }

        @Test
        @DisplayName("securitySchemes is empty in GeneratorConfig when mojo field is null")
        void securitySchemesEmptyWhenNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "securitySchemes", null);

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertTrue(config.securitySchemes().isEmpty(),
                    "securitySchemes should be empty when mojo field is null");
        }

        @Test
        @DisplayName("securitySchemes is empty in GeneratorConfig when mojo field is an empty list")
        void securitySchemesEmptyWhenEmptyList() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();
            setField(mojo, "securitySchemes", List.of());

            GeneratorConfig config = invokePrivate(mojo, "buildConfig");

            Assertions.assertTrue(config.securitySchemes().isEmpty(),
                    "securitySchemes should be empty when mojo field is an empty list");
        }

        @Test
        @DisplayName("throws MojoExecutionException when scheme entry has no name")
        void throwsWhenSchemeNameNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.SecuritySchemeEntry bad = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            bad.setType("http"); // name intentionally left null

            setField(mojo, "securitySchemes", List.of(bad));

            Assertions.assertThrows(MojoExecutionException.class,
                    () -> invokePrivate(mojo, "buildConfig"));
        }

        @Test
        @DisplayName("throws MojoExecutionException when scheme entry has no type")
        void throwsWhenSchemeTypeNull() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.SecuritySchemeEntry bad = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            bad.setName("bearerAuth"); // type intentionally left null

            setField(mojo, "securitySchemes", List.of(bad));

            Assertions.assertThrows(MojoExecutionException.class,
                    () -> invokePrivate(mojo, "buildConfig"));
        }

        @Test
        @DisplayName("throws MojoExecutionException when scheme entry has a blank name")
        void throwsWhenSchemeNameBlank() throws Exception {
            OpenApiGeneratorMojo mojo = minimalMojo();

            OpenApiGeneratorMojo.SecuritySchemeEntry bad = new OpenApiGeneratorMojo.SecuritySchemeEntry();
            bad.setName("   ");
            bad.setType("http");

            setField(mojo, "securitySchemes", List.of(bad));

            Assertions.assertThrows(MojoExecutionException.class,
                    () -> invokePrivate(mojo, "buildConfig"));
        }
    }
}
