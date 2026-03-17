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

import io.github.rspereiratech.openapi.generator.core.OpenApiGeneratorImpl;
import io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig;
import io.github.rspereiratech.openapi.generator.core.config.OutputFormat;
import io.github.rspereiratech.openapi.generator.core.config.SecuritySchemeConfig;
import io.github.rspereiratech.openapi.generator.core.config.ServerConfig;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven goal that generates an OpenAPI YAML specification from the compiled
 * Spring MVC controllers of the current project.
 *
 * <p>The goal is bound to the {@code process-classes} phase by default so that
 * the compiled controller classes are available on the classpath at generation
 * time.  It builds a child {@link java.net.URLClassLoader} from the project's
 * compile and runtime classpath elements and sets it as the thread context
 * class-loader before delegating to {@link io.github.rspereiratech.openapi.generator.core.OpenApiGenerator}.
 *
 * <h3>Minimal configuration example</h3>
 * <pre>{@code
 * <plugin>
 *   <groupId>io.github.rspereiratech</groupId>
 *   <artifactId>openapi-generator-maven-plugin</artifactId>
 *   <configuration>
 *     <basePackages>
 *       <basePackage>com.example.myapp.controller</basePackage>
 *     </basePackages>
 *     <title>My API</title>
 *     <version>1.0.0</version>
 *   </configuration>
 * </plugin>
 * }</pre>
 *
 * <h3>Multiple servers</h3>
 * <pre>{@code
 * <servers>
 *   <server>
 *     <url>https://api.example.com</url>
 *     <description>Production</description>
 *   </server>
 *   <server>
 *     <url>https://staging.example.com</url>
 *     <description>Staging</description>
 *   </server>
 * </servers>
 * }</pre>
 *
 * <h3>Single server (shorthand, backward-compatible)</h3>
 * <pre>{@code
 * <serverUrl>https://api.example.com</serverUrl>
 * }</pre>
 *
 * When both {@code <servers>} and {@code <serverUrl>} are provided,
 * {@code <servers>} takes precedence.
 *
 * <h3>Custom controller annotations</h3>
 * <p>Use {@code <controllerAnnotations>} when your project defines a custom
 * stereotype annotation that does <em>not</em> transitively meta-annotate
 * {@code @RestController} or {@code @Controller}:
 * <pre>{@code
 * <controllerAnnotations>
 *   <controllerAnnotation>com.example.annotation.MyApiEndpoint</controllerAnnotation>
 * </controllerAnnotations>
 * }</pre>
 *
 * <h3>Application name (context path)</h3>
 * <p>Use {@code <contextPath>} to append a context path to every server URL.
 * Setting {@code vcc-superx-api} turns {@code https://api.example.com} into
 * {@code https://api.example.com/vcc-superx-api/}:
 * <pre>{@code
 * <contextPath>vcc-superx-api</contextPath>
 * }</pre>
 *
 * <h3>Skipping the goal</h3>
 * <p>Set {@code -Dopenapi.generator.skip=true} on the command line, or add
 * {@code <skip>true</skip>} inside {@code <configuration>}.
  *
 * @author ruispereira
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        threadSafe = true
)
public class OpenApiGeneratorMojo extends AbstractMojo {

    // ------------------------------------------------------------------
    // Required parameters
    // ------------------------------------------------------------------

    /** Base packages to scan for Spring MVC controllers. At least one is required. */
    @Parameter(required = true)
    private List<String> basePackages;

    // ------------------------------------------------------------------
    // Optional parameters
    // ------------------------------------------------------------------

    /** Path of the generated YAML file. Defaults to {@code target/docs/swagger/openapi.yaml}. */
    @Parameter(defaultValue = "docs/swagger/openapi.yaml", property = "openapi.generator.outputFile")
    private File outputFile;

    /** API title written into the {@code info} block. */
    @Parameter(defaultValue = "API")
    private String title;

    /** API description written into the {@code info} block. */
    @Parameter(defaultValue = "")
    private String description;

    /** API version written into the {@code info} block. */
    @Parameter(defaultValue = "1.0.0", property = "openapi.generator.version")
    private String version;

    /**
     * List of server entries for the {@code servers} block.
     * Each entry accepts {@code <url>}, {@code <description>}, and
     * {@code <variables>} (see class javadoc for the full XML syntax).
     * Takes precedence over {@code <serverUrl>}.
     */
    @Parameter
    private List<ServerEntry> servers;

    /**
     * Shorthand for a single server URL (backward-compatible).
     * Ignored when {@code <servers>} is present.
     */
    @Parameter(defaultValue = "/", property = "openapi.generator.serverUrl")
    private String serverUrl;

    /** Contact name for the {@code info.contact} block. */
    @Parameter
    private String contactName;

    /** Contact e-mail for the {@code info.contact} block. */
    @Parameter
    private String contactEmail;

    /** Contact URL for the {@code info.contact} block. */
    @Parameter
    private String contactUrl;

    /** License name for the {@code info.license} block. */
    @Parameter
    private String licenseName;

    /** License URL for the {@code info.license} block. */
    @Parameter
    private String licenseUrl;

    /**
     * Additional annotation fully-qualified names to treat as controller stereotypes,
     * in addition to the built-in {@code @RestController} and {@code @Controller}.
     *
     * <p>Use this when your project defines a custom annotation that does <em>not</em>
     * transitively meta-annotate a Spring controller stereotype:
     * <pre>{@code
     * <controllerAnnotations>
     *   <controllerAnnotation>com.example.annotation.MyApiEndpoint</controllerAnnotation>
     * </controllerAnnotations>
     * }</pre>
     *
     * <p>Custom annotations that already meta-annotate {@code @RestController} (e.g. via
     * {@code @CustomRestController}) are detected automatically without this setting.
     */
    @Parameter
    private List<String> controllerAnnotations;

    /**
     * Optional security schemes to include under {@code components/securitySchemes}.
     *
     * <p>Each entry is also added to the root {@code security} list automatically.
     * Example — Bearer JWT:
     * <pre>{@code
     * <securitySchemes>
     *   <securityScheme>
     *     <name>bearerAuth</name>
     *     <type>http</type>
     *     <scheme>bearer</scheme>
     *     <bearerFormat>JWT</bearerFormat>
     *   </securityScheme>
     * </securitySchemes>
     * }</pre>
     *
     * <p>Example — API Key in header:
     * <pre>{@code
     * <securityScheme>
     *   <name>apiKeyAuth</name>
     *   <type>apiKey</type>
     *   <in>header</in>
     *   <parameterName>X-API-Key</parameterName>
     * </securityScheme>
     * }</pre>
     */
    @Parameter
    private List<SecuritySchemeEntry> securitySchemes;

    /**
     * Output format for the generated OpenAPI specification.
     * Accepted values (case-insensitive): {@code YAML} (default), {@code JSON}.
     *
     * <pre>{@code
     * <outputFormat>JSON</outputFormat>
     * }</pre>
     *
     * Can also be set via {@code -Dopenapi.generator.outputFormat=JSON} on the command line.
     */
    @Parameter(defaultValue = "YAML", property = "openapi.generator.outputFormat")
    private String outputFormat = "YAML";

    /**
     * Optional application name appended to every server URL as a path segment.
     *
     * <p>When set, each server URL is suffixed with {@code /<contextPath>/}.
     * For example, setting {@code vcc-superx-api} turns {@code https://api.example.com}
     * into {@code https://api.example.com/vcc-superx-api/}.
     *
     * <pre>{@code
     * <contextPath>vcc-superx-api</contextPath>
     * }</pre>
     *
     * <p>Omit or leave blank to use server URLs as-is.
     */
    @Parameter(property = "openapi.generator.contextPath")
    private String contextPath;

    /**
     * When {@code true}, controllers are sorted alphabetically by canonical class name
     * before processing, and the resulting paths are sorted alphabetically in the final
     * spec. This guarantees a deterministic output regardless of filesystem or JVM ordering,
     * which is especially useful in CI environments where file discovery order can vary.
     *
     * <p>Defaults to {@code false} to preserve discovery order.
     *
     * <pre>{@code
     * <sortOutput>true</sortOutput>
     * }</pre>
     */
    @Parameter(defaultValue = "false", property = "openapi.generator.sortOutput")
    private boolean sortOutput;

    /**
     * When {@code true} (default), the built-in set of framework-injected parameter types
     * (e.g. {@code java.util.Locale}, {@code jakarta.servlet.http.HttpServletRequest},
     * {@code java.security.Principal}) is silently skipped during parameter processing.
     * Set to {@code false} to disable this behaviour.
     *
     * <pre>{@code
     * <ignoreDefaultParamTypes>false</ignoreDefaultParamTypes>
     * }</pre>
     */
    @Parameter(defaultValue = "true", property = "openapi.generator.ignoreDefaultParamTypes")
    private boolean ignoreDefaultParamTypes = true;

    /**
     * Additional fully-qualified class names of parameter types to ignore, on top of
     * the built-in defaults (when {@code ignoreDefaultParamTypes} is {@code true}).
     *
     * <pre>{@code
     * <additionalIgnoredParamTypes>
     *   <additionalIgnoredParamType>com.example.MyCustomContext</additionalIgnoredParamType>
     * </additionalIgnoredParamTypes>
     * }</pre>
     */
    @Parameter
    private List<String> additionalIgnoredParamTypes;

    /**
     * Default media type for response bodies when no {@code produces} attribute is declared
     * on the handler method and no {@code mediaType} is set in {@code @Content}.
     * Mirrors {@code springdoc.default-produces-media-type}. Defaults to {@code *}{@code /*}.
     *
     * <pre>{@code
     * <defaultProducesMediaType>application/json</defaultProducesMediaType>
     * }</pre>
     */
    @Parameter(defaultValue = "*/*", property = "openapi.generator.defaultProducesMediaType")
    private String defaultProducesMediaType = "*/*";

    /**
     * Default media type for request bodies when no {@code consumes} attribute is declared
     * on the handler method.
     * Mirrors {@code springdoc.default-consumes-media-type}. Defaults to
     * {@code application/json}.
     *
     * <pre>{@code
     * <defaultConsumesMediaType>application/json</defaultConsumesMediaType>
     * }</pre>
     */
    @Parameter(defaultValue = "application/json", property = "openapi.generator.defaultConsumesMediaType")
    private String defaultConsumesMediaType = "application/json";

    /** Set to {@code true} to skip the execution of this goal entirely. */
    @Parameter(defaultValue = "false", property = "openapi.generator.skip")
    private boolean skip;

    // ------------------------------------------------------------------
    // Maven infrastructure
    // ------------------------------------------------------------------

    @Component
    private MavenProject project;

    // ------------------------------------------------------------------
    // Mojo execution
    // ------------------------------------------------------------------

    /**
     * Entry point invoked by the Maven lifecycle.
     *
     * <p>The execution proceeds in the following steps:
     * <ol>
     *   <li>If {@code skip} is {@code true}, logs a message and returns immediately.</li>
     *   <li>Calls {@code validateParameters()} to ensure required fields are present.</li>
     *   <li>Calls {@code buildConfig()} to assemble a {@link io.github.rspereiratech.openapi.generator.core.config.GeneratorConfig}.</li>
     *   <li>Builds a {@link java.net.URLClassLoader} from the project's compile and runtime classpath.</li>
     *   <li>Installs the project class-loader as the thread context class-loader.</li>
     *   <li>Delegates to {@link io.github.rspereiratech.openapi.generator.core.OpenApiGenerator#generate} to run the
     *       full generation pipeline and write the YAML output file.</li>
     *   <li>Restores the original context class-loader in a {@code finally} block.</li>
     * </ol>
     *
     * @throws MojoExecutionException if parameter validation fails, the classpath cannot be
     *                                resolved, or the generation pipeline throws an exception
     * @throws MojoFailureException   not thrown by this implementation; declared for API
     *                                compatibility with {@link org.apache.maven.plugin.AbstractMojo}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("OpenAPI generator is skipped (openapi.generator.skip=true).");
            return;
        }

        validateParameters();

        GeneratorConfig config = buildConfig();
        getLog().info("Generating OpenAPI spec from packages: " + basePackages);

        ClassLoader projectClassLoader = buildProjectClassLoader();
        ClassLoader savedContextLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(projectClassLoader);
            new OpenApiGeneratorImpl().generate(config, projectClassLoader);
        } catch (Exception e) {
            throw new MojoExecutionException("OpenAPI generation failed: " + e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(savedContextLoader);
        }
    }

    // ------------------------------------------------------------------
    // Config assembly
    // ------------------------------------------------------------------

    /**
     * Validates that all required Mojo parameters are present and non-blank.
     *
     * @throws MojoExecutionException if {@code <basePackages>} is missing, empty,
     *                                or contains a blank entry
     */
    private void validateParameters() throws MojoExecutionException {
        if (basePackages == null || basePackages.isEmpty()) {
            throw new MojoExecutionException(
                    "<basePackages> must contain at least one <basePackage> entry.");
        }
        for (String pkg : basePackages) {
            if (pkg == null || pkg.isBlank()) {
                throw new MojoExecutionException("<basePackage> values must not be blank.");
            }
        }
    }

    /**
     * Assembles a {@link GeneratorConfig} from the Mojo's bound parameters.
     *
     * <p>When {@code <servers>} is non-empty it takes precedence over the
     * {@code <serverUrl>} shorthand. Each {@link ServerEntry} is validated and
     * converted via {@link #toServerConfig(ServerEntry)}.
     *
     * @return the fully constructed {@link GeneratorConfig}
     * @throws MojoExecutionException if a server entry has a blank URL
     */
    private GeneratorConfig buildConfig() throws MojoExecutionException {
        OutputFormat format;
        try {
            format = OutputFormat.valueOf(outputFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(
                    "<outputFormat> must be YAML or JSON, got: " + outputFormat);
        }

        GeneratorConfig.Builder builder = GeneratorConfig.builder()
                .basePackages(basePackages)
                .outputFile(outputFile.getAbsolutePath())
                .title(title)
                .description(description)
                .version(version)
                .contactName(contactName)
                .contactEmail(contactEmail)
                .contactUrl(contactUrl)
                .licenseName(licenseName)
                .licenseUrl(licenseUrl)
                .outputFormat(format)
                .contextPath(contextPath)
                .sortOutput(sortOutput)
                .ignoreDefaultParamTypes(ignoreDefaultParamTypes)
                .defaultProducesMediaType(defaultProducesMediaType)
                .defaultConsumesMediaType(defaultConsumesMediaType);

        if (additionalIgnoredParamTypes != null && !additionalIgnoredParamTypes.isEmpty()) {
            builder.additionalIgnoredParamTypes(additionalIgnoredParamTypes);
        }

        if (controllerAnnotations != null && !controllerAnnotations.isEmpty()) {
            builder.controllerAnnotations(controllerAnnotations);
        }

        if (securitySchemes != null && !securitySchemes.isEmpty()) {
            for (SecuritySchemeEntry entry : securitySchemes) {
                builder.securityScheme(toSecuritySchemeConfig(entry));
            }
        }

        // <servers> takes precedence over the shorthand <serverUrl>
        if (servers != null && !servers.isEmpty()) {
            for (ServerEntry entry : servers) {
                builder.server(toServerConfig(entry));
            }
        } else {
            builder.serverUrl(serverUrl);
        }

        return builder.build();
    }

    /**
     * Converts an XML-bound {@link SecuritySchemeEntry} to a {@link SecuritySchemeConfig}.
     *
     * @param entry the security scheme entry read from the Maven configuration block
     * @return the corresponding {@link SecuritySchemeConfig}
     * @throws MojoExecutionException if the entry's {@code name} or {@code type} is {@code null} or blank
     */
    private SecuritySchemeConfig toSecuritySchemeConfig(SecuritySchemeEntry entry) throws MojoExecutionException {
        if (entry.getName() == null || entry.getName().isBlank()) {
            throw new MojoExecutionException("Each <securityScheme> entry must have a non-blank <name>.");
        }
        if (entry.getType() == null || entry.getType().isBlank()) {
            throw new MojoExecutionException("Each <securityScheme> entry must have a non-blank <type>.");
        }
        return new SecuritySchemeConfig(
                entry.getName(), entry.getType(), entry.getScheme(), entry.getBearerFormat(),
                entry.getDescription(), entry.getIn(), entry.getParameterName(), entry.getOpenIdConnectUrl());
    }

    /**
     * Converts an XML-bound {@link ServerEntry} to a {@link ServerConfig}.
     *
     * @param entry the server entry read from the Maven configuration block
     * @return the corresponding {@link ServerConfig}
     * @throws MojoExecutionException if the entry's URL is {@code null} or blank
     */
    private ServerConfig toServerConfig(ServerEntry entry) throws MojoExecutionException {
        if (entry.getUrl() == null || entry.getUrl().isBlank()) {
            throw new MojoExecutionException("Each <server> entry must have a non-blank <url>.");
        }
        return ServerConfig.of(entry.getUrl(), entry.getDescription());
    }

    // ------------------------------------------------------------------
    // Classloader
    // ------------------------------------------------------------------

    /**
     * Builds a {@link URLClassLoader} containing the project's compile and runtime
     * classpath elements, with this Mojo's own class-loader as parent.
     *
     * <p>This is necessary because the Maven plugin runs in Maven's own class-loader,
     * which cannot see the project's compiled classes.  The returned class-loader is
     * also set as the thread context class-loader before calling the generator so that
     * Jackson's {@code ModelConverters} can discover DTO classes via SPI.
     *
     * @return a class-loader that can load the project's compiled classes
     * @throws MojoExecutionException if Maven cannot resolve the project classpath
     */
    private ClassLoader buildProjectClassLoader() throws MojoExecutionException {
        List<URL> urls = new ArrayList<>();
        try {
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
            for (String element : project.getRuntimeClasspathElements()) {
                URL url = new File(element).toURI().toURL();
                if (!urls.contains(url)) urls.add(url);
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Could not resolve project classpath: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("Error building project classloader: " + e.getMessage(), e);
        }
        getLog().debug("Project classpath entries: " + urls.size());
        return new URLClassLoader(urls.toArray(URL[]::new), getClass().getClassLoader());
    }

    // ------------------------------------------------------------------
    // XML-bound POJOs  (Maven binds these from the <configuration> block)
    // ------------------------------------------------------------------

    /**
     * Represents one {@code <server>} entry inside the {@code <servers>} configuration block.
     *
     * <p>Maven binds the XML child elements to this POJO's setters:
     * <pre>{@code
     * <server>
     *   <url>https://api.example.com</url>
     *   <description>Production</description>
     * </server>
     * }</pre>
     *
     * <p>The {@code <url>} element is mandatory; {@code <description>} is optional.
     * If {@code <url>} is blank or absent, the goal will throw a
     * {@link MojoExecutionException} during config assembly.
     */
    @Getter
    @Setter
    public static class ServerEntry {
        /** The server URL (e.g. {@code "https://api.example.com"}). Must not be blank. */
        private String url;
        /** Optional human-readable description (e.g. {@code "Production"}). */
        private String description;
    }

    /**
     * Represents one {@code <securityScheme>} entry inside the {@code <securitySchemes>}
     * configuration block.
     *
     * <p>Maven binds the XML child elements to this POJO's setters:
     * <pre>{@code
     * <securityScheme>
     *   <name>bearerAuth</name>
     *   <type>http</type>
     *   <scheme>bearer</scheme>
     *   <bearerFormat>JWT</bearerFormat>
     * </securityScheme>
     * }</pre>
     *
     * <p>{@code <name>} and {@code <type>} are mandatory; all other fields are optional.
     */
    @Getter
    @Setter
    public static class SecuritySchemeEntry {
        /** Scheme name — key in {@code components/securitySchemes} and the {@code security} list. */
        private String name;
        /** Scheme type: {@code http}, {@code apiKey}, {@code oauth2}, or {@code openIdConnect}. */
        private String type;
        /** HTTP authorisation scheme (e.g. {@code bearer}, {@code basic}); used for {@code type=http}. */
        private String scheme;
        /** Token format hint (e.g. {@code JWT}); used for {@code type=http} + {@code scheme=bearer}. */
        private String bearerFormat;
        /** Human-readable description of the scheme. */
        private String description;
        /** API key location: {@code header}, {@code query}, or {@code cookie}; used for {@code type=apiKey}. */
        private String in;
        /** Header or query parameter name carrying the API key; used for {@code type=apiKey}. */
        private String parameterName;
        /** OpenID Connect well-known URL; used for {@code type=openIdConnect}. */
        private String openIdConnectUrl;
    }
}
