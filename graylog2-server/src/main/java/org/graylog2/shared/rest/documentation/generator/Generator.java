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
package org.graylog2.shared.rest.documentation.generator;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.jakarta.factories.SchemaFactoryWrapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.graylog2.shared.ServerVersion;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * This is generating API information in legacy <a href="http://swagger.io/">Swagger 1.2</a> format.
 *
 * This generator has been updated to read OpenAPI 3.x annotations (io.swagger.v3.oas.annotations.*)
 * but still produces the legacy Swagger 1.2 JSON format for backward compatibility.
 *
 * We decided to write this ourselves and not to use the Swagger JAX-RS/Jersey integration
 * because it was not compatible to Jersey2 at that point and just way too complicated
 * and too big for what we want to do with it.
 */
public class Generator {
    private static final String INNER_CLASSES_SEPARATOR = "__";

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

    public static final String EMULATED_SWAGGER_VERSION = "1.2";

    private static final Map<String, Object> overviewResult = Maps.newHashMap();
    private static final String PROPERTIES = "properties";
    private static final String ADDITIONAL_PROPERTIES = "additional_properties";
    private static final String ITEMS = "items";
    private static final String REF = "$ref";
    private static final String TYPE = "type";
    private static final String GENERIC_CLASSES_SEPARATOR = "_";
    private static final String ROUTE_SEPARATOR = "/";
    private static final String PATH = "path";

    private final Set<Class<?>> resourceClasses;
    private final Map<Class<?>, String> pluginMapping;
    private final String pluginPathPrefix;
    private final ObjectMapper mapper;
    private final boolean isCloud;
    private final boolean prefixPlugins;

    public Generator(Set<Class<?>> resourceClasses,
                     Map<Class<?>, String> pluginMapping,
                     String pluginPathPrefix,
                     ObjectMapper mapper,
                     boolean isCloud,
                     boolean prefixPlugins) {
        this.resourceClasses = resourceClasses;
        this.pluginMapping = pluginMapping;
        this.pluginPathPrefix = pluginPathPrefix;
        this.mapper = mapper.copy().registerModule(new Jdk8Module());
        this.isCloud = isCloud;
        this.prefixPlugins = prefixPlugins;
    }

    public Generator(Set<Class<?>> resourceClasses, ObjectMapper mapper, boolean isCloud, boolean prefixPlugins) {
        this(resourceClasses, ImmutableMap.of(), "", mapper, isCloud, prefixPlugins);
    }

    private String prefixedPath(Class<?> resourceClass, @Nullable String resourceAnnotationPath) {
        final String resourcePath = nullToEmpty(resourceAnnotationPath);
        final StringBuilder prefixedPath = new StringBuilder();

        if (pluginMapping.containsKey(resourceClass)) {
            prefixedPath.append(pluginPathPrefix)
                    .append(ROUTE_SEPARATOR)
                    .append(pluginMapping.get(resourceClass));
        }

        if (!resourcePath.startsWith(ROUTE_SEPARATOR)) {
            prefixedPath.append(ROUTE_SEPARATOR);
        }

        return prefixedPath.append(resourcePath).toString();
    }

    public synchronized Map<String, Object> generateOverview() {
        if (!overviewResult.isEmpty()) {
            return overviewResult;
        }

        final List<Map<String, Object>> apis = Lists.newArrayList();
        for (Class<?> clazz : getAnnotatedClasses()) {
            Tag info = clazz.getAnnotation(Tag.class);
            Path path = clazz.getAnnotation(Path.class);

            if (info == null || path == null) {
                LOG.debug("Skipping REST resource with no Tag or Path annotation: <{}>", clazz.getCanonicalName());
                continue;
            }

            final String prefixedPath = prefixedPath(clazz, path.value());
            if (isCloud && !clazz.isAnnotationPresent(PublicCloudAPI.class)) {
                LOG.info("Hiding in cloud: {}", prefixedPath);
                continue;
            }

            final Map<String, Object> apiDescription = Maps.newHashMap();
            apiDescription.put("name", (prefixPlugins && prefixedPath.startsWith(pluginPathPrefix)) ? "Plugins/" + info.name() : info.name());
            apiDescription.put(PATH, prefixedPath);
            apiDescription.put("description", info.description());

            apis.add(apiDescription);
        }
        apis.sort((o1, o2) -> ComparisonChain.start().compare(o1.get("name").toString(), o2.get("name").toString()).result());

        overviewResult.put("apiVersion", ServerVersion.VERSION.toString());
        overviewResult.put("swaggerVersion", EMULATED_SWAGGER_VERSION);
        overviewResult.put("apis", apis);

        return overviewResult;
    }

    public Set<Class<?>> getAnnotatedClasses() {
        return resourceClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(Tag.class))
                .collect(Collectors.toSet());
    }

    public Map<String, Object> generateForRoute(String route, String basePath) {
        Map<String, Object> result = Maps.newHashMap();
        Map<String, Object> models = Maps.newHashMap();
        Set<Type> modelTypes = Sets.newHashSet();
        List<Map<String, Object>> apis = Lists.newArrayList();

        for (Class<?> clazz : getAnnotatedClasses()) {
            Path path = clazz.getAnnotation(Path.class);
            if (path == null) {
                LOG.debug("Skipping REST resource with no Api or Path annotation: <{}>", clazz.getCanonicalName());
                continue;
            }

            final String prefixedPath = prefixedPath(clazz, path.value());

            if (cleanRoute(route).equals(cleanRoute(prefixedPath))) {
                // This is the class representing the given route. Get all methods.
                LOG.debug("Found corresponding REST resource class: <{}>", clazz.getCanonicalName());

                Method[] methods = clazz.getDeclaredMethods();
                if (methods == null || methods.length == 0) {
                    LOG.debug("REST resource <{}> has no methods. Skipping.", clazz.getCanonicalName());
                    break;
                }

                for (Method method : methods) {
                    if (!method.isAnnotationPresent(Operation.class)) {
                        LOG.debug("Method <{}> has no Operation annotation. Skipping.", method.toGenericString());
                        continue;
                    }

                    Operation apiOperation = method.getAnnotation(Operation.class);

                    Map<String, Object> api = Maps.newHashMap();
                    List<Map<String, Object>> operations = Lists.newArrayList();

                    String methodPath;
                    if (method.isAnnotationPresent(Path.class)) {
                        // Method has annotated Path.
                        methodPath = cleanRoute(method.getAnnotation(Path.class).value());

                        if (clazz.isAnnotationPresent(Path.class)) {
                            // The class has a Path, too. Prepend.
                            String classPath = cleanRoute(prefixedPath(clazz, clazz.getAnnotation(Path.class).value()));
                            methodPath = classPath + methodPath;
                        }
                    } else {
                        // Method has no annotated Path. We read from it's class.
                        if (clazz.isAnnotationPresent(Path.class)) {
                            methodPath = cleanRoute(prefixedPath(clazz, clazz.getAnnotation(Path.class).value()));
                        } else {
                            LOG.debug("Method <{}> has no Path annotation. Skipping.", method.toGenericString());
                            continue;
                        }
                    }

                    Produces produces = null;
                    if (clazz.isAnnotationPresent(Produces.class) || method.isAnnotationPresent(Produces.class)) {
                        produces = clazz.getAnnotation(Produces.class);
                        if (method.isAnnotationPresent(Produces.class)) {
                            produces = method.getAnnotation(Produces.class);
                        }
                    }
                    api.put(PATH, methodPath);

                    Map<String, Object> operation = Maps.newHashMap();
                    operation.put("method", determineHttpMethod(method));
                    operation.put("summary", apiOperation.summary());
                    operation.put("notes", apiOperation.description());
                    operation.put("nickname", Strings.isNullOrEmpty(apiOperation.operationId())
                            ? method.getName()
                            : apiOperation.operationId());
                    if (produces != null) {
                        operation.put("produces", produces.value());
                    }
                    // OpenAPI @Operation doesn't have a response() attribute
                    // Try to extract response type from @ApiResponse annotation first (for methods returning generic Response)
                    TypeSchema responseType = extractResponseTypeFromApiResponse(method);
                    // Fall back to extracting from method return type if no schema found in @ApiResponse
                    if (responseType == null) {
                        responseType = extractResponseType(method);
                    }
                    if (responseType != null) {
                        models.putAll(responseType.models());
                        if (responseType.name() != null && isObjectSchema(responseType.type())) {
                            operation.put(TYPE, responseType.name());
                            models.put(responseType.name(), responseType.type());
                        } else {
                            if (responseType.type() != null) {
                                operation.putAll(responseType.type());
                            } else {
                                operation.put(TYPE, responseType.name());
                            }
                        }
                    }

                    List<Parameter> parameters = determineParameters(method);
                    if (parameters != null && !parameters.isEmpty()) {
                        operation.put("parameters", parameters);
                    }

                    for (Parameter parameter : parameters) {
                        final TypeSchema parameterTypeSchema = parameter.getTypeSchema();
                        if (parameterTypeSchema.name() != null && parameterTypeSchema.type() != null) {
                            models.put(parameterTypeSchema.name(), parameterTypeSchema.type());
                        }
                        models.putAll(parameterTypeSchema.models());
                    }

                    operation.put("responseMessages", determineResponses(method));

                    operations.add(operation);
                    api.put("operations", operations);

                    apis.add(api);
                }
            }
        }

        if (basePath.endsWith(ROUTE_SEPARATOR)) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        Collections.sort(apis, (o1, o2) -> ComparisonChain.start()
                .compare(o1.get(PATH).toString(), o2.get(PATH).toString())
                .result());

        // generate the json schema for the auto-mapped return types
        for (Type type : modelTypes) {
            final Class<?> typeClass = classForType(type);
            final TypeSchema modelSchema = typeSchema(type);
            if (modelSchema.type() != null) {
                models.put(typeClass.getSimpleName(), modelSchema.type());
            }
            models.putAll(modelSchema.models());
        }
        result.put("apis", apis);
        result.put("basePath", basePath);
        result.put("models", models);
        result.put("resourcePath", cleanRoute(route));
        result.put("apiVersion", ServerVersion.VERSION.toString());
        result.put("swaggerVersion", EMULATED_SWAGGER_VERSION);

        return result;
    }

    interface TypeSchema {
        String name();

        Map<String, Object> type();

        Map<String, Object> models();
    }

    private TypeSchema createTypeSchema(String name, Map<String, Object> type, Map<String, Object> models) {
        return new TypeSchema() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Map<String, Object> type() {
                return type;
            }

            @Override
            public Map<String, Object> models() {
                return models;
            }
        };
    }

    private TypeSchema createPrimitiveSchema(String name) {
        return createTypeSchema(name, null, Collections.emptyMap());
    }

    private TypeSchema extractResponseType(Method method) {
        final Type genericReturnType = method.getGenericReturnType();
        return typeSchema(genericReturnType);
    }

    private TypeSchema extractResponseTypeFromApiResponse(Method method) {
        final ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
        if (apiResponses != null) {
            for (ApiResponse response : apiResponses.value()) {
                // Look for success response codes (200, 201, 202, etc.)
                final String code = response.responseCode();
                if (code.startsWith("20")) {
                    // Try to extract schema from content
                    final io.swagger.v3.oas.annotations.media.Content[] content = response.content();
                    if (content != null && content.length > 0) {
                        final io.swagger.v3.oas.annotations.media.Schema schema = content[0].schema();
                        if (schema != null) {
                            final Class<?> implementation = schema.implementation();
                            if (implementation != null && !implementation.equals(Void.class)) {
                                return typeSchema(implementation);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Class<?> classForType(Type type) {
        return TypeToken.of(type).getRawType();
    }

    private Type[] typeParameters(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return parameterizedType.getActualTypeArguments();
        }
        return new Type[0];
    }

    private TypeSchema typeSchema(Type genericType) {
        final Class<?> returnType = classForType(genericType);
        if (returnType.isAssignableFrom(Response.class)) {
            return createPrimitiveSchema("any");
        }

        if (returnType.isEnum()) {
            return createTypeSchema(null, schemaForType(genericType), Collections.emptyMap());
        }

        if (returnType.isAssignableFrom(StreamingOutput.class)) {
            return createPrimitiveSchema("string");
        }

        if (returnType.isAssignableFrom(FormDataBodyPart.class) || returnType.isAssignableFrom(FormDataContentDisposition.class)) {
            return createPrimitiveSchema("File");
        }

        if (isPrimitive(returnType)) {
            return createPrimitiveSchema(mapPrimitives(returnType.getSimpleName()));
        }

        if (returnType.isAssignableFrom(Map.class)) {
            final Type valueType = typeParameters(genericType)[1];
            final Map<String, Object> models = new HashMap<>();

            final String valueName;
            final Map<String, Object> modelItemsDefinition;
            if (valueType instanceof Class && isPrimitive((Class<?>) valueType)) {
                valueName = mapPrimitives(((Class<?>) valueType).getSimpleName());
                modelItemsDefinition = Collections.singletonMap(ADDITIONAL_PROPERTIES, valueName);
            } else {
                final TypeSchema valueSchema = typeSchema(valueType);
                if (valueSchema == null) {
                    return null;
                }
                valueName = valueSchema.name();
                models.putAll(valueSchema.models());
                modelItemsDefinition = Collections.singletonMap(ADDITIONAL_PROPERTIES, Collections.singletonMap(REF, valueName));
                if (valueSchema.type() != null) {
                    models.put(valueName, valueSchema.type());
                }
                models.putAll(valueSchema.models());
            }

            final String modelName = valueName + "Map";
            final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                    .put(TYPE, "object")
                    .put("id", modelName)
                    .put(PROPERTIES, Collections.emptyMap())
                    .putAll(modelItemsDefinition)
                    .build();
            models.put(modelName, model);
            return createTypeSchema(modelName, Collections.singletonMap(TYPE, modelName), models);
        }
        if (returnType.isAssignableFrom(Optional.class)) {
            final Type valueType = typeParameters(genericType)[0];
            return typeSchema(valueType);
        }
        if (returnType.isAssignableFrom(List.class) || returnType.isAssignableFrom(Set.class)) {
            final Type valueType = typeParameters(genericType)[0];
            final Map<String, Object> models = new HashMap<>();
            final String valueName;
            final Map<String, Object> modelItemsDefinition;
            if (valueType instanceof Class && isPrimitive((Class<?>) valueType)) {
                valueName = mapPrimitives(((Class<?>) valueType).getSimpleName());
                modelItemsDefinition = Collections.singletonMap(ITEMS, valueName);
            } else {
                final TypeSchema valueSchema = typeSchema(valueType);
                if (valueSchema == null) {
                    return null;
                }
                valueName = valueSchema.name();
                if (valueSchema.type() != null) {
                    models.put(valueName, valueSchema.type());
                }
                models.putAll(valueSchema.models());
                //final String valueModelId = (String)((Map<String, Object>)models.get(valueName)).get("id");
                modelItemsDefinition = Collections.singletonMap(ITEMS, Collections.singletonMap(REF, valueName));
            }
            final String modelName = valueName + "Array";
            final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                    .put(TYPE, "array")
                    .put("id", modelName)
                    .put(PROPERTIES, Collections.emptyMap())
                    .putAll(modelItemsDefinition)
                    .build();
            models.put(modelName, model);
            return createTypeSchema(modelName, Collections.singletonMap(TYPE, modelName), models);
        }

        final String modelName = uniqueModelName(genericType, returnType);
        final Map<String, Object> genericTypeSchema = schemaForType(genericType);
        if (!isObjectOrArray(genericTypeSchema)) {
            return createTypeSchema(null, genericTypeSchema, Collections.emptyMap());
        }

        final TypeSchema inlineSchema = extractInlineModels(genericTypeSchema);
        return createTypeSchema(modelName, inlineSchema.type(), inlineSchema.models());
    }

    private String uniqueModelName(Type genericType, Class<?> returnType) {
        final var simpleName = nestedNames(returnType).collect(Collectors.joining(INNER_CLASSES_SEPARATOR));
        if (genericType instanceof ParameterizedType parameterizedType) {
            final var classNames = Arrays.stream(parameterizedType.getActualTypeArguments())
                    .map(type -> uniqueModelName(type, classForType(type)))
                    .toList();
            return simpleName + GENERIC_CLASSES_SEPARATOR + Joiner.on(GENERIC_CLASSES_SEPARATOR).join(classNames);
        }
        return simpleName;
    }

    private Stream<String> nestedNames(Class<?> returnType) {
        if (returnType.getEnclosingClass() == null) {
            return Stream.of(returnType.getSimpleName());
        }
        return Stream.concat(nestedNames(returnType.getEnclosingClass()), Stream.of(returnType.getSimpleName()));
    }

    private TypeSchema extractInlineModels(Map<String, Object> genericTypeSchema) {
        if (isObjectSchema(genericTypeSchema)) {
            final Map<String, Object> newGenericTypeSchema = new HashMap<>(genericTypeSchema);
            final Map<String, Object> models = new HashMap<>();
            if (genericTypeSchema.get(PROPERTIES) instanceof Map) {
                final Map<String, Object> properties = (Map<String, Object>) genericTypeSchema.get(PROPERTIES);
                final Map<String, Object> newProperties = properties.entrySet().stream().map(entry -> {
                            final Map<String, Object> property = (Map<String, Object>) entry.getValue();
                            final TypeSchema propertySchema = extractInlineModels(property);
                            models.putAll(propertySchema.models());
                            final Map<String, Object> type = reuseTypeRef(propertySchema.type());
                            if (propertySchema.name() == null) {
                                return new AbstractMap.SimpleEntry<>(entry.getKey(), type);
                            }
                            if (propertySchema.type() != null) {
                                models.put(propertySchema.name(), type);
                            }
                            return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), Collections.singletonMap(REF, propertySchema.name()));
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                newGenericTypeSchema.put(PROPERTIES, newProperties);
            }
            if (genericTypeSchema.get(ADDITIONAL_PROPERTIES) instanceof Map) {
                final Map<String, Object> additionalProperties = (Map<String, Object>) genericTypeSchema.get(ADDITIONAL_PROPERTIES);
                final TypeSchema itemsSchema = extractInlineModels(additionalProperties);
                models.putAll(itemsSchema.models());
                if (itemsSchema.name() != null) {
                    if (itemsSchema.type() != null) {
                        models.put(itemsSchema.name(), itemsSchema.type());
                    }
                    newGenericTypeSchema.put(ADDITIONAL_PROPERTIES, Collections.singletonMap(REF, itemsSchema.name()));
                } else {
                    if (itemsSchema.type() != null) {
                        final Map<String, Object> type = reuseTypeRef(itemsSchema.type());
                        newGenericTypeSchema.put(ADDITIONAL_PROPERTIES, itemsSchema.type());
                    }
                }
            }

            if (!genericTypeSchema.containsKey(PROPERTIES)) {
                newGenericTypeSchema.put(PROPERTIES, Collections.emptyMap());
            }
            final String id = shortenJsonSchemaURNs((String) genericTypeSchema.get("id"));
            return createTypeSchema(id, newGenericTypeSchema, models);
        }

        if (isArraySchema(genericTypeSchema)) {
            final Map<String, Object> models = new HashMap<>();
            final Map<String, Object> newGenericTypeSchema = new HashMap<>(genericTypeSchema);
            if (genericTypeSchema.get(ITEMS) instanceof Map) {
                final Map<String, Object> items = (Map<String, Object>) genericTypeSchema.get(ITEMS);
                final TypeSchema itemsSchema = extractInlineModels(items);
                models.putAll(itemsSchema.models());
                if (itemsSchema.name() != null) {
                    if (itemsSchema.type() != null) {
                        models.put(itemsSchema.name(), itemsSchema.type());
                    }
                    newGenericTypeSchema.put(ITEMS, Collections.singletonMap(REF, itemsSchema.name()));
                } else {
                    final Map<String, Object> type = reuseTypeRef(itemsSchema.type());
                    newGenericTypeSchema.put(ITEMS, type);
                }
            }
            return createTypeSchema(null, newGenericTypeSchema, models);
        }
        return createTypeSchema(null, genericTypeSchema, Collections.emptyMap());
    }

    private Map<String, Object> reuseTypeRef(Map<String, Object> type) {
        if (type.get(REF) != null) {
            type.put(REF, shortenJsonSchemaURNs((String) type.get(REF)));
        }

        return type;
    }

    private static final Pattern IDENT =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_$:]*");

    private static final Set<String> KEYWORDS =
            Set.of("extends", "super");

    private List<String> splitIfGeneric(String genericFqcn) {
        if (genericFqcn == null) {
            return null;
        }
        final List<String> result = new ArrayList<>();
        final var m = IDENT.matcher(genericFqcn);
        while (m.find()) {
            final var token = m.group();
            if (!KEYWORDS.contains(token) && !token.equals("?")) {
                result.add(token);
            }
        }
        return result;
    }

    private String shortenJsonSchemaURNs(@Nullable String id) {
        final var genericParts = splitIfGeneric(id);
        return genericParts != null ? genericParts.stream().map(this::shortenJsonSchemaURN).collect(Collectors.joining(GENERIC_CLASSES_SEPARATOR)) : null;
    }
    private String shortenJsonSchemaURN(@Nullable String id) {
        if (id == null) {
            return null;
        }
        final Splitter splitter = Splitter.on(":");
        final List<String> segments = splitter.splitToList(id);
        if (segments.isEmpty()) {
            return id;
        }
        return segments.stream()
                .filter(segment -> Character.isUpperCase(segment.codePointAt(0)))
                .collect(Collectors.joining(INNER_CLASSES_SEPARATOR));
    }

    private static Optional<String> typeOfSchema(@Nullable Map<String, Object> typeSchema) {
        return Optional.ofNullable(typeSchema)
                .map(schema -> Strings.emptyToNull((String) schema.get(TYPE)));
    }

    private static boolean isArraySchema(Map<String, Object> genericTypeSchema) {
        return typeOfSchema(genericTypeSchema).map(type -> type.equals("array")).orElse(false);
    }

    private static boolean isObjectSchema(Map<String, Object> genericTypeSchema) {
        return typeOfSchema(genericTypeSchema).map(type -> type.equals("object")).orElse(false);
    }

    private Map<String, Object> schemaForType(Type valueType) {
        final SchemaFactoryWrapper schemaFactoryWrapper = new CustomSchemaFactoryWrapper();
        final JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(mapper, schemaFactoryWrapper);
        try {
            final JsonSchema schema = schemaGenerator.generateSchema(mapper.getTypeFactory().constructType(valueType));
            final Map<String, Object> schemaMap = mapper.readValue(mapper.writeValueAsBytes(schema), Map.class);
            if (schemaMap.containsKey(ADDITIONAL_PROPERTIES) && !schemaMap.containsKey(PROPERTIES)) {
                schemaMap.put(PROPERTIES, Collections.emptyMap());
            }
            if (schemaMap.equals(Collections.singletonMap(TYPE, "any"))) {
                return ImmutableMap.of(
                        TYPE, "object",
                        PROPERTIES, Collections.emptyMap()
                );
            }
            return schemaMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isObjectOrArray(Map<String, Object> schemaMap) {
        return isObjectSchema(schemaMap) || isArraySchema(schemaMap);
    }

    private List<Parameter> determineParameters(Method method) {
        final List<Parameter> params = Lists.newArrayList();

        int i = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            final Parameter param = new Parameter();

            Parameter.Kind paramKind = Parameter.Kind.BODY;
            for (Annotation annotation : annotations) {
                // Note: Can't import Parameter annotation due to name collision with inner class
                if (annotation instanceof io.swagger.v3.oas.annotations.Parameter paramAnnotation) {
                    final String name = Strings.isNullOrEmpty(paramAnnotation.name())
                            ? "arg" + i
                            : paramAnnotation.name();
                    param.setName(name);
                    param.setDescription(paramAnnotation.description());
                    param.setIsRequired(paramAnnotation.required());

                    final TypeSchema parameterSchema = typeSchema(method.getGenericParameterTypes()[i]);
                    param.setTypeSchema(parameterSchema);

                    // defaultValue is not in OpenAPI @Parameter - only use JAX-RS @DefaultValue

                    // allowableValues moved to schema.allowableValues
                    final Schema schema = paramAnnotation.schema();
                    if (schema != null && schema.allowableValues() != null && schema.allowableValues().length > 0) {
                        final List<String> allowableValues = Arrays.asList(schema.allowableValues());
                        param.setAllowableValues(allowableValues);
                    }
                }

                // Support OpenAPI 3.x @RequestBody annotation for body parameters (semantically correct)
                if (annotation instanceof RequestBody requestBodyAnnotation) {
                    param.setName("JSON body");
                    param.setDescription(requestBodyAnnotation.description());
                    param.setIsRequired(requestBodyAnnotation.required());

                    final TypeSchema parameterSchema = typeSchema(method.getGenericParameterTypes()[i]);
                    param.setTypeSchema(parameterSchema);
                }

                if (annotation instanceof DefaultValue) {
                    final DefaultValue defaultValueAnnotation = (DefaultValue) annotation;

                    // Only set if empty to make sure ApiParam's defaultValue has precedence!
                    if (isNullOrEmpty(param.getDefaultValue()) && !isNullOrEmpty(defaultValueAnnotation.value())) {
                        param.setDefaultValue(defaultValueAnnotation.value());
                    }
                }

                if (annotation instanceof QueryParam) {
                    paramKind = Parameter.Kind.QUERY;
                    final String annotationValue = ((QueryParam) annotation).value();
                    param.setName(annotationValue);
                } else if (annotation instanceof PathParam) {
                    final String annotationValue = ((PathParam) annotation).value();
                    if (!Strings.isNullOrEmpty(annotationValue)) {
                        param.setName(annotationValue);
                    }
                    paramKind = Parameter.Kind.PATH;
                } else if (annotation instanceof HeaderParam) {
                    paramKind = Parameter.Kind.HEADER;
                    final String annotationValue = ((HeaderParam) annotation).value();
                    param.setName(annotationValue);
                } else if (annotation instanceof FormParam) {
                    paramKind = Parameter.Kind.FORM;
                    final String annotationValue = ((FormParam) annotation).value();
                    param.setName(annotationValue);
                } else if (annotation instanceof FormDataParam) {
                    paramKind = Parameter.Kind.FORMDATA;
                    final String annotationValue = ((FormDataParam) annotation).value();
                    param.setName(annotationValue);
                }
            }

            param.setKind(paramKind);

            if (param.getTypeSchema() != null) {
                params.add(param);
            }

            i++;
        }

        return params;
    }

    class PrimitiveType implements Type {
        private final String type;

        PrimitiveType(String type) {
            this.type = type;
        }

        @Override
        public String getTypeName() {
            return type;
        }
    }

    private List<Map<String, Object>> determineResponses(Method method) {
        final List<Map<String, Object>> result = Lists.newArrayList();

        final ApiResponses annotation = method.getAnnotation(ApiResponses.class);
        if (null != annotation) {
            for (ApiResponse response : annotation.value()) {
                // Note: responseCode is now a String, not int; description instead of message
                try {
                    final Map<String, Object> responseDescription = ImmutableMap.<String, Object>of(
                            "code", Integer.parseInt(response.responseCode()),
                            "message", response.description());

                    result.add(responseDescription);
                } catch (NumberFormatException e) {
                    LOG.warn("Could not parse response code '{}' for method {}", response.responseCode(), method.getName());
                }
            }
        }

        return result;
    }

    // Leading slash but no trailing.
    private String cleanRoute(String route) {
        if (!route.startsWith(ROUTE_SEPARATOR)) {
            route = ROUTE_SEPARATOR + route;
        }

        if (route.endsWith(ROUTE_SEPARATOR)) {
            route = route.substring(0, route.length() - 1);
        }

        return route;
    }

    private static final Set<String> PRIMITIVES = ImmutableSet.of(
            "boolean",
            "Boolean",
            "Double",
            "Float",
            "int",
            "Integer",
            "Long",
            "long",
            "Number",
            "Object",
            "String",
            "void",
            "Void"
    );

    private static boolean isPrimitive(String simpleName) {
        return PRIMITIVES.contains(simpleName);
    }

    private static boolean isPrimitive(Class<?> klass) {
        return isPrimitive(klass.getSimpleName());
    }

    private static String mapPrimitives(String simpleName) {
        if (Strings.isNullOrEmpty(simpleName)) {
            return simpleName;
        }

        switch (simpleName) {
            case "int":
            case "Integer":
            case "Long":
                return "integer";
            case "Number":
            case "Float":
            case "Double":
                return "number";
            case "String":
                return "string";
            case "boolean":
            case "Boolean":
                return "boolean";
            case "Void":
            case "void":
                return "void";
            case "Object":
                return "any";
        }
        return simpleName;
    }

    @Nullable
    private String determineHttpMethod(Method m) {
        if (m.isAnnotationPresent(GET.class)) {
            return "GET";
        }

        if (m.isAnnotationPresent(POST.class)) {
            return "POST";
        }

        if (m.isAnnotationPresent(PUT.class)) {
            return "PUT";
        }

        if (m.isAnnotationPresent(PATCH.class)) {
            return "PATCH";
        }

        if (m.isAnnotationPresent(DELETE.class)) {
            return "DELETE";
        }

        if (m.isAnnotationPresent(HEAD.class)) {
            return "HEAD";
        }

        if (m.isAnnotationPresent(OPTIONS.class)) {
            return "OPTIONS";
        }

        return null;
    }

    public static class Parameter {
        private String name;
        private String description;
        private boolean isRequired;
        private TypeSchema typeSchema;
        private Kind kind;
        private String defaultValue;
        private Collection<String> allowableValues;

        public void setName(String name) {
            this.name = name;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setIsRequired(boolean required) {
            isRequired = required;
        }

        public void setRequired(boolean required) {
            isRequired = required;
        }

        public void setTypeSchema(TypeSchema typeSchema) {
            this.typeSchema = typeSchema;
        }

        public TypeSchema getTypeSchema() {
            return typeSchema;
        }

        private String getType() {
            return mapPrimitives(typeSchema.name());
        }

        public void setKind(Kind kind) {
            this.kind = kind;
        }

        private String getKind() {
            return kind.toString().toLowerCase(Locale.ENGLISH);
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public void setAllowableValues(Collection<String> allowableValues) {
            this.allowableValues = allowableValues;
        }

        @JsonValue
        public Map<String, Object> jsonValue() {
            final HashMap<String, Object> result = new HashMap<>();
            result.put("name", name);
            result.put("description", description);
            result.put("required", isRequired);
            result.put("paramType", getKind());

            if (defaultValue != null) {
                result.put("defaultValue", defaultValue);
            }

            if (allowableValues != null) {
                result.put("enum", allowableValues);
            }

            if (typeSchema.type() == null || isObjectSchema(typeSchema.type())) {
                result.put(TYPE, typeSchema.name());
            } else {
                result.putAll(typeSchema.type());
            }

            return ImmutableMap.copyOf(result);
        }

        public enum Kind {
            BODY,
            HEADER,
            PATH,
            QUERY,
            FORM,
            FORMDATA
        }
    }
}
