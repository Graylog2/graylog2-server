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
package org.graylog.plugins.views.visualizations;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.path.NodePath;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.jsonschema.AllowedValuesModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * The visualizations of aggregation widgets and their settings, for tools that create and change widgets, e.g. the
 * Assistant's. Settings are described by the JSON Schema of the visualization's config class, and checked against it.
 */
@Singleton
public class VisualizationConfigs {
    /** The path under which errors name settings, as tools take them. */
    public static final String SETTINGS = "visualization_config";
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    // Possessive and without overlapping classes, so it cannot backtrack.
    private static final Pattern QUALIFIED_TYPE = Pattern.compile("`(?:\\w++[.$])*+(\\w++)`");

    private final Map<String, VisualizationDescriptor> visualizations;
    private final ObjectMapper objectMapper;
    private final SchemaGenerator schemaGenerator;
    private final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private final Map<String, Settings> settings = new ConcurrentHashMap<>();

    public record VisualizationOptions(@JsonProperty("name") String name,
                                       @JsonProperty("title") String title,
                                       @JsonProperty("description") String description,
                                       @JsonProperty("settings") @JsonInclude(JsonInclude.Include.NON_NULL)
                                       @Nullable JsonNode settings) {}

    private record Settings(ObjectNode schema, Schema validator, Map<String, Object> defaults) {}

    @Inject
    public VisualizationConfigs(Map<String, VisualizationDescriptor> visualizations, ObjectMapper objectMapper) {
        this.visualizations = Map.copyOf(visualizations);
        this.objectMapper = objectMapper;
        this.schemaGenerator = new SchemaGenerator(schemaGeneratorConfig(objectMapper));
    }

    /** All visualizations, by name. */
    public List<VisualizationDescriptor> list() {
        return visualizations.values().stream().sorted(Comparator.comparing(VisualizationDescriptor::name)).toList();
    }

    public Optional<VisualizationDescriptor> get(String name) {
        return Optional.ofNullable(visualizations.get(name));
    }

    /** The visualization and the JSON Schema of its settings, with their defaults; none if it has no settings. */
    public VisualizationOptions options(String name) {
        final VisualizationDescriptor visualization = require(name);
        return new VisualizationOptions(visualization.name(), visualization.title(), visualization.description(),
                visualization.hasSettings() ? settings(visualization).schema().deepCopy() : null);
    }

    /** The names of the visualization's top-level settings. */
    public List<String> settingNames(String name) {
        final VisualizationDescriptor visualization = require(name);
        if (!visualization.hasSettings()) {
            return List.of();
        }
        final List<String> names = new ArrayList<>();
        settings(visualization).schema().path("properties").fieldNames().forEachRemaining(names::add);
        return names;
    }

    /** The settings of a new widget showing the visualization; empty if it has none. */
    public Map<String, Object> defaults(String name) {
        final VisualizationDescriptor visualization = require(name);
        return visualization.hasSettings() ? settings(visualization).defaults() : Map.of();
    }

    /**
     * Checks changed settings against the visualization's schema. A null value resets a setting.
     *
     * @throws InvalidVisualizationConfigException with every problem, by the path of the setting
     */
    public void check(String name, Map<String, Object> changes) {
        final VisualizationDescriptor visualization = require(name);
        if (changes.isEmpty()) {
            return;
        }
        if (!visualization.hasSettings()) {
            throw new InvalidVisualizationConfigException(List.of(f("%s: %s has no settings", SETTINGS, name)));
        }
        final JsonNode given = objectMapper.valueToTree(withoutNulls(changes));
        final List<String> errors = settings(visualization).validator().validate(given).stream()
                .map(error -> describe(visualization, error))
                .distinct()
                .toList();
        if (!errors.isEmpty()) {
            throw new InvalidVisualizationConfigException(errors);
        }
    }

    /**
     * Applies changed settings to the previous ones, or to the defaults if there are none, e.g. for a new widget.
     * Nested settings are merged, lists replaced, and a null value resets a setting.
     *
     * @return the complete settings, as the visualization's config class writes them; null if it has no settings
     * @throws InvalidVisualizationConfigException with every problem, by the path of the setting
     */
    @Nullable
    public Map<String, Object> apply(String name, @Nullable Map<String, Object> previous, Map<String, Object> changes) {
        check(name, changes);
        final VisualizationDescriptor visualization = require(name);
        if (!visualization.hasSettings()) {
            return null;
        }
        final Map<String, Object> base = previous == null || previous.isEmpty() ? defaults(name) : previous;
        final Map<String, Object> merged = merge(base, changes);
        try {
            // Lenient about settings only the web interface knows; they are dropped, as when the view is saved.
            final Object config = objectMapper.readerFor(visualization.configClass())
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue((JsonNode) objectMapper.valueToTree(merged));
            return objectMapper.convertValue(config, MAP);
        } catch (JsonMappingException e) {
            throw new InvalidVisualizationConfigException(List.of(describe(e)));
        } catch (IOException e) {
            throw new InvalidVisualizationConfigException(List.of(f("%s: %s", SETTINGS, e.getMessage())));
        }
    }

    private VisualizationDescriptor require(String name) {
        return get(name).orElseThrow(() -> new IllegalArgumentException(f("Unknown visualization <%s>; known are %s",
                name, visualizations.keySet().stream().sorted().collect(Collectors.joining(", ")))));
    }

    private Settings settings(VisualizationDescriptor visualization) {
        return settings.computeIfAbsent(visualization.name(), name -> {
            final Map<String, Object> defaults = defaultsOf(visualization);
            final ObjectNode schema = schemaGenerator.generateSchema(visualization.configClass());
            schema.remove(List.of("$schema", "description"));
            simplify(schema);
            final JsonNode properties = schema.path("properties");
            defaults.forEach((setting, value) -> {
                if (value != null && properties.get(setting) instanceof ObjectNode property) {
                    property.set("default", objectMapper.valueToTree(value));
                }
            });
            return new Settings(schema, schemaRegistry.getSchema(schema), defaults);
        });
    }

    private Map<String, Object> defaultsOf(VisualizationDescriptor visualization) {
        try {
            final Object config = objectMapper.readerFor(visualization.configClass()).readValue("{}");
            return objectMapper.convertValue(config, MAP);
        } catch (IOException e) {
            throw new IllegalStateException(f("The config class of visualization <%s> needs a default for every "
                    + "setting", visualization.name()), e);
        }
    }

    // Tools change some settings at a time, so none is required; the previous settings or the defaults give the
    // others. The generator splits members whose type has its own attributes, e.g. axis configs, into an allOf.
    private static void simplify(JsonNode schema) {
        if (schema instanceof ObjectNode object) {
            object.remove("required");
            if (object.remove("allOf") instanceof ArrayNode parts) {
                parts.forEach(part -> part.properties().forEach(entry -> {
                    if (!object.has(entry.getKey())) {
                        object.set(entry.getKey(), entry.getValue());
                    }
                }));
            }
        }
        schema.forEach(VisualizationConfigs::simplify);
    }

    private String describe(VisualizationDescriptor visualization, Error error) {
        final String path = path(error.getInstanceLocation());
        return switch (error.getKeyword()) {
            case "additionalProperties" -> path.equals(SETTINGS)
                    ? f("%s.%s: %s has no such setting; its settings are %s", SETTINGS, error.getProperty(),
                    visualization.name(), String.join(", ", settingNames(visualization.name())))
                    : f("%s.%s: no such setting", path, error.getProperty());
            case "enum" -> f("%s: must be one of %s", path, values(error.getSchemaNode()));
            case "propertyNames" -> f("%s.%s: no such key; the keys are %s", path, error.getProperty(),
                    values(error.getSchemaNode().path("enum")));
            case "type" -> f("%s: must be of type %s", path, error.getSchemaNode().isArray()
                    ? values(error.getSchemaNode())
                    : error.getSchemaNode().asText());
            default -> f("%s: %s", path, message(error));
        };
    }

    private static String path(NodePath location) {
        final StringBuilder path = new StringBuilder(SETTINGS);
        for (int i = 0; i < location.getNameCount(); i++) {
            final Object element = location.getElement(i);
            path.append(element instanceof Integer index ? "[" + index + "]" : "." + element);
        }
        return path.toString();
    }

    private static String values(JsonNode values) {
        return values.isArray()
                ? StreamSupport.stream(values.spliterator(), false).map(JsonNode::asText)
                .collect(Collectors.joining(", "))
                : values.asText();
    }

    // Messages start with the instance location, which the path replaces.
    private static String message(Error error) {
        final String message = error.getMessage();
        final int separator = message.indexOf(": ");
        return separator < 0 ? message : message.substring(separator + 2);
    }

    private static String describe(JsonMappingException e) {
        final String path = e.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? "." + reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("", SETTINGS, ""));
        final String message = withSimpleTypeNames(e.getOriginalMessage()).lines().findFirst().orElse("invalid value");
        return f("%s: %s", path, message);
    }

    // Jackson quotes types by their qualified names, e.g. `org.graylog.Dto$Mode`, which mean nothing to tools.
    static String withSimpleTypeNames(String message) {
        return QUALIFIED_TYPE.matcher(message).replaceAll("$1");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> changes) {
        final Map<String, Object> merged = new LinkedHashMap<>(base);
        changes.forEach((key, value) -> {
            if (value == null) {
                merged.remove(key);
            } else if (value instanceof Map<?, ?> nested && merged.get(key) instanceof Map<?, ?> previous) {
                merged.put(key, merge((Map<String, Object>) previous, (Map<String, Object>) nested));
            } else {
                merged.put(key, value);
            }
        });
        return merged;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> withoutNulls(Map<String, Object> changes) {
        final Map<String, Object> result = new HashMap<>();
        changes.forEach((key, value) -> {
            if (value instanceof Map<?, ?> nested) {
                result.put(key, withoutNulls((Map<String, Object>) nested));
            } else if (value != null) {
                result.put(key, value);
            }
        });
        return result;
    }

    private static SchemaGeneratorConfig schemaGeneratorConfig(ObjectMapper objectMapper) {
        final var builder = new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2020_12,
                OptionPreset.PLAIN_JSON)
                .with(Option.FIELDS_DERIVED_FROM_ARGUMENTFREE_METHODS, Option.NONSTATIC_NONVOID_NONGETTER_METHODS,
                        Option.FLATTENED_OPTIONALS, Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES,
                        Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT, Option.INLINE_ALL_SCHEMAS)
                .with(new JacksonModule(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE))
                .with(new AllowedValuesModule());

        // Only what Jackson writes: @JsonProperty members, named as the mapper names them.
        builder.forMethods()
                .withIgnoreCheck(method -> method.getAnnotationConsideringFieldAndGetter(JsonProperty.class) == null
                        || method.getDeclaringType().getMemberFields().stream()
                        .anyMatch(field -> field.getName().equals(method.getName())))
                .withPropertyNameOverrideResolver(method -> propertyName(objectMapper,
                        method.getAnnotationConsideringFieldAndGetter(JsonProperty.class), method.getName()));
        builder.forFields()
                .withIgnoreCheck(field -> field.getAnnotationConsideringFieldAndGetter(JsonProperty.class) == null)
                .withPropertyNameOverrideResolver(field -> propertyName(objectMapper,
                        field.getAnnotationConsideringFieldAndGetter(JsonProperty.class), field.getName()));

        // Maps keyed by an enum, e.g. axis configs by axis, only take its values as keys.
        builder.forTypesInGeneral().withTypeAttributeOverride((attributes, scope, context) -> {
            final ResolvedType type = scope.getType();
            if (!type.isInstanceOf(Map.class)) {
                return;
            }
            final ResolvedType key = context.getTypeContext().getTypeParameterFor(type, Map.class, 0);
            if (key != null && key.isInstanceOf(Enum.class)) {
                final ArrayNode keys = attributes.putObject("propertyNames").putArray("enum");
                for (Object constant : key.getErasedType().getEnumConstants()) {
                    keys.add(objectMapper.valueToTree(constant).asText());
                }
            }
        });
        return builder.build();
    }

    private static String propertyName(ObjectMapper objectMapper, @Nullable JsonProperty property, String memberName) {
        if (property != null && !property.value().isEmpty()) {
            return property.value();
        }
        final String name = memberName.endsWith("()") ? memberName.substring(0, memberName.length() - 2) : memberName;
        return objectMapper.getPropertyNamingStrategy() instanceof PropertyNamingStrategies.NamingBase naming
                ? naming.translate(name)
                : name;
    }
}
