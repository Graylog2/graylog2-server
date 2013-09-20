/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.rest.documentation.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.rest.documentation.annotations.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is generating API information in Swagger format.
 *
 * http://swagger.wordnik.com/
 *
 * We decided to write this ourselves and not to use the Swagger JAXRS/Jersey integration
 * because it was not compatible to Jersey2 at that point and just way too complicated
 * and too big for what we want to do with it.
 *
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Generator {

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

    public static final String EMULATED_SWAGGER_VERSION = "1.2";

    private Object lock = new Object();

    private static Map<String, Object> overviewResult = Maps.newHashMap();
    private static Reflections reflections;

    private final String packageName;

    public Generator(String packageName) {
        this.packageName = packageName;

        synchronized (lock) {
            if (reflections == null) {
                reflections = new Reflections(packageName);
            }
        }
    }

    public Map<String, Object> generateOverview() {
        synchronized (overviewResult) {
            if (!overviewResult.isEmpty()) {
                return overviewResult;
            }

            List<Map<String, Object>> apis = Lists.newArrayList();
            for (Class<?> clazz : getAnnotatedClasses()) {
                Api info = clazz.getAnnotation(Api.class);
                Path path = clazz.getAnnotation(Path.class);

                if (info == null || path == null) {
                    LOG.debug("Skipping REST resource with no Api or Path annotation: <{}>", clazz.getCanonicalName());
                    continue;
                }

                Map<String, Object> apiDescription = Maps.newHashMap();
                apiDescription.put("name", info.value());
                apiDescription.put("path", path.value());
                apiDescription.put("description", info.description());

                apis.add(apiDescription);
            }

            Map<String, String> info = Maps.newHashMap();
            info.put("title", "Graylog2 REST API");

            overviewResult.put("apiVersion", Core.GRAYLOG2_VERSION);
            overviewResult.put("swaggerVersion", EMULATED_SWAGGER_VERSION);
            overviewResult.put("apis", apis);

            return overviewResult;
        }
    }

    public Set<Class<?>> getAnnotatedClasses() {
        return reflections.getTypesAnnotatedWith(Api.class);
    }

    public Map<String, Object> generateForRoute(String route, String basePath) {
        Map<String, Object> result = Maps.newHashMap();

        List<Map<String, Object>> apis = Lists.newArrayList();
        for (Class<?> clazz : getAnnotatedClasses()) {
            Path path = clazz.getAnnotation(Path.class);
            if (path == null) {
                LOG.debug("Skipping REST resource with no Api or Path annotation: <{}>", clazz.getCanonicalName());
                continue;
            }

            if(cleanRoute(route).equals(cleanRoute(path.value()))) {
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
                            String classPath = cleanRoute(clazz.getAnnotation(Path.class).value());
                            methodPath = classPath + methodPath;
                        }
                    } else {
                        // Method has no annotated Path. We read from it's class.
                        if (clazz.isAnnotationPresent(Path.class)) {
                            methodPath = cleanRoute(clazz.getAnnotation(Path.class).value());
                        } else {
                            LOG.debug("Method <{}> has no Path annotation. Skipping.", method.toGenericString());
                            continue;
                        }
                    }

                    api.put("path", methodPath);

                    Map<String, Object> operation = Maps.newHashMap();
                    operation.put("method", determineHttpMethod(method));
                    operation.put("summary", apiOperation.value());
                    operation.put("notes", apiOperation.notes());
                    operation.put("nickname", method.getName());
                    operation.put("parameters", determineParameters(method));
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

        result.put("apis", apis);
        result.put("basePath", basePath);
        result.put("resourcePath", cleanRoute(route));
        result.put("apiVersion", Core.GRAYLOG2_VERSION);
        result.put("swaggerVersion", EMULATED_SWAGGER_VERSION);

        return result;
    }

    private List<Map<String, Object>> determineParameters(Method method) {
        List<Map<String, Object>> parameters = Lists.newArrayList();

        int i = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            Map<String, Object> parameter = Maps.newHashMap();

            for (Annotation annotation : annotations) {
                if (annotation instanceof ApiParam) {
                    ApiParam apiParam = (ApiParam) annotation;
                    parameter.put("name", apiParam.title());
                    parameter.put("description", apiParam.description());
                    parameter.put("required", apiParam.required());

                    Type parameterClass = method.getGenericParameterTypes()[i];
                    if(parameterClass.equals(String.class)) {
                        parameter.put("type", "string");
                    } else if(parameterClass.equals(int.class) || parameterClass.equals(Integer.class)) {
                        parameter.put("type", "integer");
                    }
                }

                String paramType = "";
                if (annotation instanceof QueryParam) {
                    paramType = "query";
                }

                if (annotation instanceof PathParam) {
                    paramType = "path";
                }

                parameter.put("paramType", paramType);
            }

            parameters.add(parameter);
            i++;
        }

        return parameters;
    }

    private List<Map<String, Object>> determineResponses(Method method) {
        List<Map<String, Object>> result = Lists.newArrayList();

        if (method.isAnnotationPresent(ApiResponses.class)) {
            ApiResponses responses = method.getAnnotation(ApiResponses.class);
            for(ApiResponse response : responses.value()) {
                Map<String, Object> responseDescription = Maps.newHashMap();

                responseDescription.put("code", response.code());
                responseDescription.put("message", response.message());

                result.add(responseDescription);
            }
        }

        return result;
    }

    // Leading slash but no trailing.
    private String cleanRoute(String route) {
        if(!route.startsWith("/")) {
            route = "/" + route;
        }

        if (route.endsWith("/")) {
            route = route.substring(0, route.length() - 1);
        }

        return route;
    }

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

        if (m.isAnnotationPresent(DELETE.class)) {
            return "DELETE";
        }

        return null;
    }

}
