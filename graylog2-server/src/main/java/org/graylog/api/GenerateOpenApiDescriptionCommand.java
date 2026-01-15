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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.graylog2.plugin.inject.Graylog2Module.SYSTEM_REST_RESOURCES;
import static org.graylog2.shared.utilities.StringUtils.f;

@Command(name = "openapi-description", description = "Generates an OpenAPI description for the Graylog REST API.")
public class GenerateOpenApiDescriptionCommand extends Server {

    private static final TypeLiteral<Class<? extends PluginRestResource>> PLUGIN_REST_RESOURCE_TYPE =
            new TypeLiteral<>() {};

    @Option(name = {"--skip-validation"}, description = "Skip validation of the generated OpenAPI description.")
    private boolean skipValidation = false;

    @Arguments(title = "Output file path", description = "File to write the OpenAPI description to. " +
            "If the file already exists, it will be silently overwritten. " +
            "The format of the generated description will be determined by the file extension (json or yaml).")
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
        // in the constructors of our components. We only want to indescriptiont the guice bindings
        final var moduleElements = Elements.getElements(modules);

        final var pluginRestResourcesModule = extractPluginRestResourcesModule(moduleElements);
        final var systemRestResourcesModule = extractSystemRestResourcesModule(moduleElements);
        final var jacksonSubTypesModule = extractJacksonSubTypesModule(moduleElements);

        // Return a minimal injector with just the bindings we need for OpenAPI generation
        // The generator's classes never need to construct any instances other than sets of class objects,
        // so this is safe, no matter what the resources actually need to have injected.
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

        System.out.println("Generating OpenAPI description...");

        final var context = openApiContextFactory.getOrCreate("generate-openapi-description-command");
        final var serialized = outputFile.endsWith(".json") ? prettyJson(context) : prettyYaml(context);

        System.out.println(f("Generation completed. [took %s ms]", generationStopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));

        if (skipValidation) {
            System.out.println("Skipping OpenAPI description validation.");
        } else {
            final var validationStopwatch = Stopwatch.createStarted();
            System.out.println("Validating OpenAPI description...");

            validate(serialized);
            System.out.println(f("Validation completed. [took %s ms]", validationStopwatch.stop().elapsed(TimeUnit.MILLISECONDS)));
        }

        final var targetPath = Path.of(outputFile).toAbsolutePath();
        final var parentPath = targetPath.getParent();

        System.out.println(f("Writing OpenAPI description to \"%s\"", targetPath));

        if (!Files.exists(parentPath)) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                System.out.println(f("Cannot create output directory \"%s\": %s", parentPath, e.getMessage()));
                System.exit(1);
            }
        }
        if ((Files.exists(targetPath) && !Files.isWritable(targetPath)) || !Files.isWritable(parentPath)) {
            System.out.println(f("Cannot write OpenAPI description. Make sure that the following path is writable: \"%s\"",
                    targetPath));
            System.exit(1);
        }
        if (Files.exists(targetPath)) {
            System.out.println("Overwriting existing OpenAPI description...");
        }

        try {
            Files.writeString(targetPath, serialized);
        } catch (Exception e) {
            throw new RuntimeException(f("Failed to write OpenAPI description to \"%s\"", targetPath), e);
        }

        System.out.println("OpenAPI description written.");
    }

    private void validate(String document) {
        final var parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        final SwaggerParseResult result = new OpenAPIV3Parser().readContents(document, null, parseOptions);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            System.err.println("OpenAPI description validation failed:");
            result.getMessages().forEach(msg -> System.err.println("  - " + msg));
            System.exit(1);
        }

        System.out.println("OpenAPI description is valid.");
    }

    private String prettyJson(OpenApiContext context) {
        return pretty(context.getOutputJsonMapper(), context.read());
    }

    private String prettyYaml(OpenApiContext context) {
        return pretty(context.getOutputYamlMapper(), context.read());
    }

    private String pretty(ObjectMapper mapper, OpenAPI description) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(description);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OpenAPI description as YAML", e);
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
