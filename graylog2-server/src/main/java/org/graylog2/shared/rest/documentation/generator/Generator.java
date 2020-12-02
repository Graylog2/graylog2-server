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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.graylog2.shared.ServerVersion;
import org.reflections.Reflections;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static Map<String, Object> overviewResult = Maps.newHashMap();
    private static Reflections reflections;

    private final Map<Class<?>, String> pluginMapping;
    private final String pluginPathPrefix;
    private final ObjectMapper mapper;

    public Generator(Set<String> packageNames, Map<Class<?>, String> pluginMapping, String pluginPathPrefix, ObjectMapper mapper) {
        this.pluginMapping = pluginMapping;
        this.pluginPathPrefix = pluginPathPrefix;
        this.mapper = mapper;

        if (reflections == null) {
            reflections = new Reflections(packageNames.toArray(),
                    pluginMapping.keySet().stream().map(Class::getClassLoader).collect(Collectors.toSet()));
        }
    }

    public Generator(String packageName, ObjectMapper mapper) {
        this(ImmutableSet.of(packageName), ImmutableMap.of(), "", mapper);
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
        return reflections.getTypesAnnotatedWith(Api.class);
    }

    public Map<String, Object> generateForRoute(String route, String basePath) {
        Map<String, Object> result = Maps.newHashMap();
        Set<Class<?>> modelTypes = Sets.newHashSet();
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
                    if (!method.getReturnType().isAssignableFrom(Response.class)) {
                        operation.put("type", method.getReturnType().getSimpleName());
                        modelTypes.add(method.getReturnType());
                    }

                    List<Parameter> parameters = determineParameters(method);
                    if (parameters != null && !parameters.isEmpty()) {
                        operation.put("parameters", parameters);
                    }
                    for (Parameter parameter : parameters) {
                        final Class type = parameter.getType();
                        if (Primitives.unwrap(type).isPrimitive() || type.equals(String.class)) {
                            continue;
                        }
                        modelTypes.add(type);
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
        Map<String, Object> models = Maps.newHashMap();
        for (Class<?> type : modelTypes) {

            // skip non-jackson mapped classes (like Response)
            if (!type.isAnnotationPresent(JsonAutoDetect.class)) {
                continue;
            }
            try {
                SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
                mapper.acceptJsonFormatVisitor(mapper.constructType(type), visitor);
                final JsonSchema schema = visitor.finalSchema();
                models.put(type.getSimpleName(), schema);
            } catch (JsonMappingException e) {
                LOG.error("Error generating model schema. Ignoring this model, this will likely break the API browser.", e);
            }

        }
        result.put("apis", apis);
        result.put("basePath", basePath);
        result.put("models", models);
        result.put("resourcePath", cleanRoute(route));
        result.put("apiVersion", ServerVersion.VERSION.toString());
        result.put("swaggerVersion", EMULATED_SWAGGER_VERSION);

        return result;
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
                    param.setType(method.getGenericParameterTypes()[i]);

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

            if (param.getType() != null) {
                params.add(param);
            }

            i++;
        }

        return params;
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
        private Class type;
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

        public void setType(Type type) {
            final Class<?> klass;

            if (type instanceof ParameterizedType) {
                klass = (Class<?>) ((ParameterizedType) type).getRawType();
            } else {
                klass = (Class<?>) type;
            }

            if (klass.isPrimitive()) {
                this.type = Primitives.wrap(klass);
            } else {
                this.type = klass;
            }
        }

        @JsonIgnore
        public Class getType() {
            return type;
        }

        @JsonProperty("type")
        public String getTypeName() {
            return type.getSimpleName();
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
