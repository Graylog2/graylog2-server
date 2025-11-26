/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Pattern;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.common.base.Stopwatch;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.graylog.grn.GRNRegistry;
import org.graylog2.commands.Server;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.inject.JacksonSubTypes;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.GraylogClassLoader;
import org.graylog2.shared.rest.documentation.openapi.OpenAPIBindings;
import org.graylog2.shared.rest.documentation.openapi.OpenAPIContextFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.graylog2.plugin.inject.Graylog2Module.SYSTEM_REST_RESOURCES;
import static org.graylog2.shared.utilities.StringUtils.f;

@Command(name = "openapi-spec", description = "Generates an OpenAPI specification for the Graylog REST API.")
public class GenerateOpenApiSpecCommand extends Server {

    private static final TypeLiteral<Class<? extends PluginRestResource>> PLUGIN_REST_RESOURCE_TYPE =
            new TypeLiteral<>() {};

    @Option(name = {"--skip-spec-validation"}, description = "Skip validation of the generated OpenAPI specification.")
    private boolean skipValidation = false;

    @Arguments(title = "Output file path", description = "File to write the OpenAPI specification to. " +
            "If the file already exists, it will be silently overwritten. " +
            "The format of the generated spec will be determined by the file extension (json or yaml).")
    @Pattern(pattern = "^.+\\.(json|yaml|yml)$", description = "Output file must have a .json, .yaml or .yml extension.")
    @Required
    private String outputFile;

    @Override
    protected void beforeInjectorCreation(Set<Plugin> plugins) {
        // Overriding to prevent running of preflight checks
    }

    @Override
    protected Injector doCreateInjector(List<Module> modules) {

        // We don't want to actually create the injector, because that would execute a lot of code that we have
        // in the constructors of our components. We only want to inspect the guice bindings
        final var moduleElements = Elements.getElements(modules);

        final var pluginRestResourcesModule = extractPluginRestResourcesModule(moduleElements);
        final var systemRestResourcesModule = extractSystemRestResourcesModule(moduleElements);
        final var jacksonSubTypesModule = extractJacksonSubTypesModule(moduleElements);

        // Return a minimal injector with just the bindings we need for OpenAPI generation
        return Guice.createInjector(Stage.PRODUCTION,
                systemRestResourcesModule, pluginRestResourcesModule, jacksonSubTypesModule, new OpenAPIBindings(),
                binder -> {
                    binder.bind(boolean.class).annotatedWith(Names.named("is_cloud")).toInstance(configuration.isCloud());
                    binder.bind(Version.class).toInstance(Version.from(0, 0, 0));
                    binder.bind(ClassLoader.class).annotatedWith(GraylogClassLoader.class).toInstance(chainingClassLoader);
                    binder.bind(EncryptedValueService.class).toInstance(new EncryptedValueService(UUID.randomUUID().toString()));
                    binder.bind(InputConfigurationBeanDeserializerModifier.class).toInstance(InputConfigurationBeanDeserializerModifier.withoutConfig());
                    binder.bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
                    binder.bind(GRNRegistry.class).toInstance(GRNRegistry.createWithBuiltinTypes());
                });
    }

    @Override
    protected void startCommand() {
        final var openApiContextFactory = injector.getInstance(OpenAPIContextFactory.class);

        final var generationStopwatch = Stopwatch.createStarted();

        System.out.println("Generating OpenAPI specification.");

        final var context = openApiContextFactory.getOrCreate("generate-openapi-spec-command");
        final var serialized = outputFile.endsWith(".json") ? prettyJson(context) : prettyYaml(context);

        System.out.println(f("Generation completed. [took %s ms]", generationStopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));

        final var validationStopwatch = Stopwatch.createStarted();
        System.out.println("Validating OpenAPI specification.");

        if (!skipValidation) {
            validateOpenApiSpec(serialized);
        }

        System.out.println(f("Validation completed. [took %s ms]", validationStopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));

        final var targetPath = Path.of(outputFile);
        final var parentPath = targetPath.getParent();

        if ((Files.exists(targetPath) && !Files.isWritable(targetPath)) || !Files.isWritable(parentPath)) {
            System.out.println("Cannot write to specified file: " + outputFile);
            System.exit(1);
        }
        if (Files.exists(targetPath)) {
            System.out.println("Overwriting existing file: " + outputFile);
        }

        try {
            Files.writeString(targetPath, serialized);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write OpenAPI spec to file " + outputFile, e);
        }

        System.out.println("OpenAPI specification written to " + outputFile);
    }

    private void validateOpenApiSpec(String serializedSpec) {
        final var parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        final SwaggerParseResult result = new OpenAPIV3Parser().readContents(serializedSpec, null, parseOptions);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            System.err.println("OpenAPI specification validation failed:");
            result.getMessages().forEach(msg -> System.err.println("  - " + msg));
            System.exit(1);
        }

        System.out.println("OpenAPI specification is valid.");
    }

    private String prettyJson(OpenApiContext context) {
        return pretty(context.getOutputJsonMapper(), context.read());
    }

    private String prettyYaml(OpenApiContext context) {
        return pretty(context.getOutputYamlMapper(), context.read());
    }

    private String pretty(ObjectMapper mapper, OpenAPI spec) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OpenAPI spec as YAML", e);
        }
    }

    private Module extractPluginRestResourcesModule(List<Element> elements) {
        final var pluginResourcesBinder = elements.stream()
                .map(element ->
                        element.acceptVisitor(new DefaultElementVisitor<MapBinderBinding<?>>() {
                            @Override
                            public <T> MapBinderBinding<?> visit(Binding<T> binding) {
                                return binding.acceptTargetVisitor(new PluginRestResourcesBinderVisitor());
                            }
                        })
                )
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No plugin REST resources binder found!"));

        final var bindings = elements.stream().filter(pluginResourcesBinder::containsElement).toList();
        return Elements.getModule(bindings);
    }

    private Module extractSystemRestResourcesModule(List<Element> elements) {
        final var resourcesBinder = elements.stream()
                .map(element ->
                        element.acceptVisitor(new DefaultElementVisitor<MultibinderBinding<?>>() {
                            @Override
                            public <T> MultibinderBinding<?> visit(Binding<T> binding) {
                                return binding.acceptTargetVisitor(new SystemRestResourcesBinderVisitor());
                            }
                        })
                )
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No system REST resources binder found!"));

        final var bindings = elements.stream().filter(resourcesBinder::containsElement).toList();
        return Elements.getModule(bindings);
    }

    private Module extractJacksonSubTypesModule(List<Element> elements) {
        final var subTypesBinder = elements.stream()
                .map(element ->
                        element.acceptVisitor(new DefaultElementVisitor<MultibinderBinding<?>>() {
                            @Override
                            public <T> MultibinderBinding<?> visit(Binding<T> binding) {
                                return binding.acceptTargetVisitor(new JacksonSubTypesBinderVisitor());
                            }
                        })
                )
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No jackson subtypes binder found!"));

        final var bindings = elements.stream().filter(subTypesBinder::containsElement).toList();
        return Elements.getModule(bindings);
    }

    // A visitor that returns the binder for plugin rest resources
    private static class PluginRestResourcesBinderVisitor
            extends DefaultBindingTargetVisitor<Object, MapBinderBinding<?>>
            implements MultibindingsTargetVisitor<Object, MapBinderBinding<?>> {

        @Override
        public MapBinderBinding<?> visit(MapBinderBinding<?> mapBinder) {
            if (mapBinder.getValueTypeLiteral().equals(PLUGIN_REST_RESOURCE_TYPE)) {
                return mapBinder;
            }
            return null;
        }

        @Override
        public MapBinderBinding<?> visit(OptionalBinderBinding<?> ignored) {
            return null;
        }

        @Override
        public MapBinderBinding<?> visit(MultibinderBinding<?> ignored) {
            return null;
        }
    }

    // A visitor that returns the binder for system rest resources
    private static class SystemRestResourcesBinderVisitor
            extends DefaultBindingTargetVisitor<Object, MultibinderBinding<?>>
            implements MultibindingsTargetVisitor<Object, MultibinderBinding<?>> {

        @Override
        public MultibinderBinding<?> visit(MapBinderBinding<?> ignored) {
            return null;
        }

        @Override
        public MultibinderBinding<?> visit(OptionalBinderBinding<?> ignored) {
            return null;
        }

        @Override
        @SuppressForbidden("Comparing Guice Annotations")
        public MultibinderBinding<?> visit(MultibinderBinding<?> multibinderBinding) {
            if (Names.named(SYSTEM_REST_RESOURCES).equals(multibinderBinding.getSetKey().getAnnotation())) {
                return multibinderBinding;
            }
            return null;
        }
    }

    // A visitor that returns the binder for jackson subtypes
    private static class JacksonSubTypesBinderVisitor
            extends DefaultBindingTargetVisitor<Object, MultibinderBinding<?>>
            implements MultibindingsTargetVisitor<Object, MultibinderBinding<?>> {

        @Override
        public MultibinderBinding<?> visit(MapBinderBinding<?> ignored) {
            return null;
        }

        @Override
        public MultibinderBinding<?> visit(OptionalBinderBinding<?> ignored) {
            return null;
        }

        @Override
        public MultibinderBinding<?> visit(MultibinderBinding<?> multibinderBinding) {
            if (JacksonSubTypes.class.equals(multibinderBinding.getSetKey().getAnnotationType())) {
                return multibinderBinding;
            }
            return null;
        }

    }
}
