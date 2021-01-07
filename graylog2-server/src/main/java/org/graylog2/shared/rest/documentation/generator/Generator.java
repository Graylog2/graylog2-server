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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.graylog2.shared.ServerVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

/**
 * This is generating API information in <a href="http://swagger.io/">Swagger</a> format.
 *
 * We decided to write this ourselves and not to use the Swagger JAX-RS/Jersey integration
 * because it was not compatible to Jersey2 at that point and just way too complicated
 * and too big for what we want to do with it.
 */
public class Generator {

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

    public static final String EMULATED_SWAGGER_VERSION = "1.2";

    private static final Map<String, Object> overviewResult = Maps.newHashMap();

    private final Set<Class<?>> resourceClasses;
    private final Map<Class<?>, String> pluginMapping;
    private final String pluginPathPrefix;
    private final ObjectMapper mapper;

    public Generator(Set<Class<?>> resourceClasses, Map<Class<?>, String> pluginMapping, String pluginPathPrefix, ObjectMapper mapper) {
        this.resourceClasses = resourceClasses;
        this.pluginMapping = pluginMapping;
        this.pluginPathPrefix = pluginPathPrefix;
        this.mapper = mapper;
    }

    public Generator(Set<Class<?>> resourceClasses, ObjectMapper mapper) {
        this(resourceClasses, ImmutableMap.of(), "", mapper);
    }

    private String prefixedPath(Class<?> resourceClass, @Nullable String resourceAnnotationPath) {
        final String resourcePath = nullToEmpty(resourceAnnotationPath);
        final StringBuilder prefixedPath = new StringBuilder();

        if (pluginMapping.containsKey(resourceClass)) {
            prefixedPath.append(pluginPathPrefix);
            prefixedPath.append("/");
            prefixedPath.append(pluginMapping.get(resourceClass));
        }

        if (!resourcePath.startsWith("/")) {
            prefixedPath.append("/");
        }

        return prefixedPath.append(resourcePath).toString();
    }

    public synchronized Map<String, Object> generateOverview() {
        if (!overviewResult.isEmpty()) {
            return overviewResult;
        }

        final List<Map<String, Object>> apis = Lists.newArrayList();
        for (Class<?> clazz : getAnnotatedClasses()) {
            Api info = clazz.getAnnotation(Api.class);
            Path path = clazz.getAnnotation(Path.class);

            if (info == null || path == null) {
                LOG.debug("Skipping REST resource with no Api or Path annotation: <{}>", clazz.getCanonicalName());
                continue;
            }

            final String prefixedPath = prefixedPath(clazz, path.value());
            final Map<String, Object> apiDescription = Maps.newHashMap();
            apiDescription.put("name", prefixedPath.startsWith(pluginPathPrefix) ? "Plugins/" + info.value() : info.value());
            apiDescription.put("path", prefixedPath);
            apiDescription.put("description", info.description());

            apis.add(apiDescription);
        }
        Collections.sort(apis, (o1, o2) -> ComparisonChain.start().compare(o1.get("name").toString(), o2.get("name").toString()).result());
        Map<String, String> info = Maps.newHashMap();
        info.put("title", "Graylog REST API");

        overviewResult.put("apiVersion", ServerVersion.VERSION.toString());
        overviewResult.put("swaggerVersion", EMULATED_SWAGGER_VERSION);
        overviewResult.put("apis", apis);

        return overviewResult;
    }

    public Set<Class<?>> getAnnotatedClasses() {
        return resourceClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(Api.class))
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
                    if (!method.isAnnotationPresent(ApiOperation.class)) {
                        LOG.debug("Method <{}> has no ApiOperation annotation. Skipping.", method.toGenericString());
                        continue;
                    }

                    ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);

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
                    api.put("path", methodPath);

                    Map<String, Object> operation = Maps.newHashMap();
                    operation.put("method", determineHttpMethod(method));
                    operation.put("summary", apiOperation.value());
                    operation.put("notes", apiOperation.notes());
                    operation.put("nickname", method.getName());
                    if (produces != null) {
                        operation.put("produces", produces.value());
                    }
                    // skip Response.class because we can't reliably infer any schema information from its payload anyway.
                    final TypeSchema responseType = extractResponseType(method);
                    if (responseType != null) {
                        if (responseType.type() != null) {
                            operation.putAll(responseType.type());
                        }
                        models.putAll(responseType.models());
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

        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        Collections.sort(apis, (o1, o2) -> ComparisonChain.start()
                .compare(o1.get("path").toString(), o2.get("path").toString())
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

    private static Class<?> classForType(Type type) {
        return TypeToken.of(type).getRawType();
    }

    private Type[] typeParameters(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)type;
            return parameterizedType.getActualTypeArguments();
        }
        return new Type[0];
    }

    private TypeSchema typeSchema(Type genericType) {
        final Class<?> returnType = classForType(genericType);
        if (returnType.isAssignableFrom(Response.class)) {
            return null;
        }

        if (returnType.isAssignableFrom(StreamingOutput.class)) {
            return createPrimitiveSchema("string");
        }

        if (isPrimitive(returnType)) {
            return createPrimitiveSchema(mapPrimitives(returnType.getSimpleName()));
        }

        if (returnType.isAssignableFrom(Map.class)) {
            final Type valueType = typeParameters(genericType)[1];
            final Map<String, Object> models = new HashMap<>();

            final String valueName;
            final Map<String, Object> modelItemsDefinition;
            if (valueType instanceof Class && isPrimitive((Class<?>)valueType)) {
                valueName = mapPrimitives(((Class<?>) valueType).getSimpleName());
                modelItemsDefinition = Collections.singletonMap("additional_properties", valueName);
            } else {
                final TypeSchema valueSchema = typeSchema(valueType);
                if (valueSchema == null) {
                    return null;
                }
                valueName = valueSchema.name();
                models.putAll(valueSchema.models());
                modelItemsDefinition = Collections.singletonMap("additional_properties", Collections.singletonMap("$ref", valueName));
                if (valueSchema.type() != null) {
                    models.put(valueName, valueSchema.type());
                }
                models.putAll(valueSchema.models());
            }

            final String modelName = valueName + "Map";
            final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                    .put("type", "object")
                    .put("id", modelName)
                    .put("properties", Collections.emptyMap())
                    .putAll(modelItemsDefinition)
                    .build();
            models.put(modelName, model);
            return createTypeSchema(modelName, Collections.singletonMap("type", modelName), models);
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
            if (valueType instanceof Class && isPrimitive((Class<?>)valueType)) {
                valueName = mapPrimitives(((Class<?>) valueType).getSimpleName());
                modelItemsDefinition = Collections.singletonMap("items", valueName);
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
                final String valueModelId = (String)((Map<String, Object>)models.get(valueName)).get("id");
                modelItemsDefinition = Collections.singletonMap("items", Collections.singletonMap("$ref", valueModelId));
            }
            final String modelName = valueName + "Array";
            final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                    .put("type", "array")
                    .put("id", modelName)
                    .put("properties", Collections.emptyMap())
                    .putAll(modelItemsDefinition)
                    .build();
            models.put(modelName, model);
            return createTypeSchema(modelName, Collections.singletonMap("type", modelName), models);
        }

        final String modelName = returnType.getSimpleName();
        final Map<String, Object> genericTypeSchema = schemaForType(genericType);
        if (!isObjectOrArray(genericTypeSchema)) {
            return createTypeSchema(modelName, ImmutableMap.of("type", modelName), ImmutableMap.of(modelName, schemaForType(genericType)));
        }

        final TypeSchema inlineSchema = extractInlineModels(genericTypeSchema);
        return createTypeSchema(modelName, inlineSchema.type(), inlineSchema.models());
    }

    private TypeSchema extractInlineModels(Map<String, Object> genericTypeSchema) {
        if (isObjectSchema(genericTypeSchema)) {
            final Map<String, Object> newGenericTypeSchema = new HashMap<>(genericTypeSchema);
            final Map<String, Object> models = new HashMap<>();
            if (genericTypeSchema.get("properties") instanceof Map) {
                final Map<String, Object> properties = (Map<String, Object>) genericTypeSchema.get("properties");
                final Map<String, Object> newProperties = properties.entrySet().stream().map(entry -> {
                    final Map<String, Object> property = (Map<String, Object>) entry.getValue();
                    final TypeSchema propertySchema = extractInlineModels(property);
                    models.putAll(propertySchema.models());
                    if (propertySchema.name() == null) {
                        return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), propertySchema.type());
                    }
                    if (propertySchema.type() != null) {
                        models.put(propertySchema.name(), propertySchema.type());
                    }
                    return new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), Collections.singletonMap("$ref", propertySchema.name()));
                })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                newGenericTypeSchema.put("properties", newProperties);
            }
            if (genericTypeSchema.get("additional_properties") instanceof Map) {
                final Map<String, Object> additionalProperties = (Map<String, Object>) genericTypeSchema.get("additional_properties");
                final TypeSchema itemsSchema = extractInlineModels(additionalProperties);
                models.putAll(itemsSchema.models());
                if (itemsSchema.name() != null) {
                    if (itemsSchema.type() != null) {
                        models.put(itemsSchema.name(), itemsSchema.type());
                    }
                    newGenericTypeSchema.put("additional_properties", Collections.singletonMap("$ref", itemsSchema.name()));
                }
            }

            if (!genericTypeSchema.containsKey("properties")) {
                newGenericTypeSchema.put("properties", Collections.emptyMap());
            }
            final String id = shortenJsonSchemaURN((String)genericTypeSchema.get("id"));
            return createTypeSchema(id, newGenericTypeSchema, models);
        }

        if (isArraySchema(genericTypeSchema)) {
            final Map<String, Object> models = new HashMap<>();
            final Map<String, Object> newGenericTypeSchema = new HashMap<>(genericTypeSchema);
            if (genericTypeSchema.get("items") instanceof Map) {
                final Map<String, Object> items = (Map<String, Object>) genericTypeSchema.get("items");
                final TypeSchema itemsSchema = extractInlineModels(items);
                models.putAll(itemsSchema.models());
                if (itemsSchema.name() != null) {
                    if (itemsSchema.type() != null) {
                        models.put(itemsSchema.name(), itemsSchema.type());
                    }
                    newGenericTypeSchema.put("items", Collections.singletonMap("$ref", itemsSchema.name()));
                }
            }
            return createTypeSchema(null, newGenericTypeSchema, models);
        }
        return createTypeSchema(null, genericTypeSchema, Collections.emptyMap());
    }

    private String shortenJsonSchemaURN(@Nullable String id) {
        if (id == null) {
            return null;
        }
        final Splitter splitter = Splitter.on(":");
        final List<String> segments = splitter.splitToList(id);
        return segments.size() > 0
                ? segments.get(segments.size() - 1)
                : id;
    }

    private boolean isArraySchema(Map<String, Object> genericTypeSchema) {
        return typeOfSchema(genericTypeSchema).equals("array");
    }

    private String typeOfSchema(Map<String, Object> typeSchema) {
        return Strings.nullToEmpty((String)typeSchema.get("type"));
    }
    private boolean isObjectSchema(Map<String, Object> genericTypeSchema) {
        return typeOfSchema(genericTypeSchema).equals("object");
    }

    private Map<String, Object> schemaForType(Type valueType) {
        final SchemaFactoryWrapper schemaFactoryWrapper = new SchemaFactoryWrapper() {
            @Override
            public JsonAnyFormatVisitor expectAnyFormat(JavaType convertedType) {
                /*final ObjectSchema s = schemaProvider.objectSchema();
                s.putProperty("anyType", schemaProvider.stringSchema());
                this.schema = s;
                return visitorFactory.anyFormatVisitor(new AnySchema());*/
                return super.expectAnyFormat(convertedType);
            }
        };
        final JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(mapper, schemaFactoryWrapper);
        try {
            final JsonSchema schema = schemaGenerator.generateSchema(mapper.getTypeFactory().constructType(valueType));
            final Map<String, Object> schemaMap = mapper.readValue(mapper.writeValueAsBytes(schema), Map.class);
            if (schemaMap.containsKey("additional_properties") && !schemaMap.containsKey("properties")) {
                schemaMap.put("properties", Collections.emptyMap());
            }
            if (schemaMap.equals(Collections.singletonMap("type", "any"))) {
                return ImmutableMap.of(
                        "type", "object",
                        "properties", Collections.emptyMap()
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
                if (annotation instanceof ApiParam) {
                    final ApiParam apiParam = (ApiParam) annotation;
                    param.setName(apiParam.name());
                    param.setDescription(apiParam.value());
                    param.setIsRequired(apiParam.required());

                    final TypeSchema parameterSchema = typeSchema(method.getGenericParameterTypes()[i]);
                    param.setTypeSchema(parameterSchema);

                    if (!isNullOrEmpty(apiParam.defaultValue())) {
                        param.setDefaultValue(apiParam.defaultValue());
                    }
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
                } else if (annotation instanceof PathParam) {
                    paramKind = Parameter.Kind.PATH;
                } else if (annotation instanceof HeaderParam) {
                    paramKind = Parameter.Kind.HEADER;
                } else if (annotation instanceof FormParam) {
                    paramKind = Parameter.Kind.FORM;
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
                final Map<String, Object> responseDescription = ImmutableMap.<String, Object>of(
                        "code", response.code(),
                        "message", response.message());

                result.add(responseDescription);
            }
        }

        return result;
    }

    // Leading slash but no trailing.
    private String cleanRoute(String route) {
        if (!route.startsWith("/")) {
            route = "/" + route;
        }

        if (route.endsWith("/")) {
            route = route.substring(0, route.length() - 1);
        }

        return route;
    }

    private final static Set<String> PRIMITIVES = ImmutableSet.of(
            "boolean",
            "Boolean",
            "Double",
            "Float",
            "int",
            "Integer",
            "Long",
            "Number",
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

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setIsRequired(boolean required) {
            isRequired = required;
        }

        public boolean isRequired() {
            return isRequired;
        }

        public void setRequired(boolean required) {
            isRequired = required;
        }

        public void setTypeSchema(TypeSchema typeSchema) {
            this.typeSchema = typeSchema;
        }

        @JsonIgnore
        public TypeSchema getTypeSchema() {
            return typeSchema;
        }

        @JsonProperty("type")
        public String getType() {
            return mapPrimitives(typeSchema.name());
        }

        public void setKind(Kind kind) {
            this.kind = kind;
        }

        @JsonProperty("paramType")
        public String getKind() {
            return kind.toString().toLowerCase(Locale.ENGLISH);
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @JsonProperty("defaultValue")
        public String getDefaultValue() {
            return this.defaultValue;
        }

        public enum Kind {
            BODY,
            HEADER,
            PATH,
            QUERY,
            FORM
        }
    }
}
