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
package org.graylog2.shared.rest;

import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.graylog2.rest.RestTools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Lists the permission declaration of every endpoint in a set of Jersey {@link Resource} models.
 * <p>
 * A declaration is one of {@link RequiresPermissions}, {@link RequiresGuest}, {@link InlinePermissionCheck},
 * {@link DelegatedPermissionCheck}, {@link ProxiedPermissionCheck} and {@link NoPermissionCheckRequired}. An endpoint
 * without any of them is undeclared. Like {@code ShiroSecurityBinding}, a {@link RequiresPermissions} on the resource
 * class counts for every method of the class, and {@link RequiresGuest} counts only on the method. If both the class
 * and the method have {@link RequiresPermissions}, both are listed, but Shiro checks only the one on the class.
 * <p>
 * Pass the models of the resource classes that the server registers. Annotations are read from the handler class of
 * the model, so a model built from an interface or abstract class misses the annotations of its implementations.
 * <p>
 * This class only reads the model. It does not change how a request is checked.
 */
public final class PermissionCoverage {
    private static final Comparator<Endpoint> ORDER = Comparator.comparing(Endpoint::path)
            .thenComparing(Endpoint::httpMethod)
            .thenComparing(Endpoint::javaMethod);
    private static final Pattern MULTIPLE_SLASHES = Pattern.compile("/{2,}");

    private PermissionCoverage() {
    }

    /**
     * Returns one row per resource method in the given resources and their child resources, sorted by path, HTTP
     * method and Java method. Sub-resource locators have no HTTP method and are not listed.
     */
    public static List<Endpoint> endpoints(Collection<Resource> resources) {
        final List<Endpoint> endpoints = new ArrayList<>();
        resources.forEach(resource -> collect(resource, endpoints));
        endpoints.sort(ORDER);
        return List.copyOf(endpoints);
    }

    private static void collect(Resource resource, List<Endpoint> endpoints) {
        for (final ResourceMethod resourceMethod : resource.getResourceMethods()) {
            final Class<?> handlerClass = resourceMethod.getInvocable().getHandler().getHandlerClass();
            final Method method = resourceMethod.getInvocable().getHandlingMethod();
            endpoints.add(new Endpoint(resourceMethod.getHttpMethod(),
                    normalizePath(RestTools.getPathFromResource(resource)),
                    handlerClass,
                    method,
                    declarations(handlerClass, method)));
        }
        resource.getChildResources().forEach(child -> collect(child, endpoints));
    }

    // Jersey keeps the slashes of each @Path value, so "/api/" and "/ca" join to "/api//ca".
    private static String normalizePath(String path) {
        final String collapsed = MULTIPLE_SLASHES.matcher(path).replaceAll("/");
        return collapsed.startsWith("/") ? collapsed : f("/%s", collapsed);
    }

    private static List<Declaration> declarations(Class<?> resourceClass, Method method) {
        final List<Declaration> declarations = new ArrayList<>();
        final RequiresPermissions classPermissions = resourceClass.getAnnotation(RequiresPermissions.class);
        if (classPermissions != null) {
            declarations.add(new Declaration(classPermissions, true));
        }
        for (final Class<? extends Annotation> type : List.of(RequiresPermissions.class, RequiresGuest.class,
                InlinePermissionCheck.class, DelegatedPermissionCheck.class, ProxiedPermissionCheck.class,
                NoPermissionCheckRequired.class)) {
            final Annotation annotation = method.getAnnotation(type);
            if (annotation != null) {
                declarations.add(new Declaration(annotation, false));
            }
        }
        return List.copyOf(declarations);
    }

    /**
     * One endpoint.
     *
     * @param httpMethod    the HTTP method, for example {@code GET}
     * @param path          the path from the {@code @Path} annotations of the class and the method, with a leading
     *                      slash and without repeated slashes. It lacks the prefix that the server adds for the API
     *                      or a plugin.
     * @param resourceClass the resource class that handles the request
     * @param method        the Java method that handles the request
     * @param declarations  the permission declarations of the endpoint, empty if it has none
     */
    public record Endpoint(String httpMethod, String path, Class<?> resourceClass, Method method,
                           List<Declaration> declarations) {
        public boolean isDeclared() {
            return !declarations.isEmpty();
        }

        /**
         * The resource class and method name, for example {@code org.graylog2.rest.resources.HelloWorldResource#helloWorld}.
         */
        public String javaMethod() {
            return f("%s#%s", resourceClass.getName(), method.getName());
        }

        /**
         * The declarations as text, for example {@code RequiresPermissions(class)+NoPermissionCheckRequired}, or
         * {@code none} if the endpoint has no declaration.
         */
        public String declarationSummary() {
            if (declarations.isEmpty()) {
                return "none";
            }
            return declarations.stream().map(Declaration::toString).collect(Collectors.joining("+"));
        }
    }

    /**
     * One permission declaration of an endpoint.
     *
     * @param annotation the annotation that declares it
     * @param classLevel {@code true} if the annotation is on the resource class rather than on the method
     */
    public record Declaration(Annotation annotation, boolean classLevel) {
        public Class<? extends Annotation> type() {
            return annotation.annotationType();
        }

        @Override
        public String toString() {
            return classLevel ? f("%s(class)", type().getSimpleName()) : type().getSimpleName();
        }
    }
}
