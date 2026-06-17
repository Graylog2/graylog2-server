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
package org.graylog2.shared.rest.documentation.openapi;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.google.inject.assistedinject.Assisted;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.initializers.JerseyService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomReader extends Reader {
    public static final String PATH_MARKER = "__HANDLED__";
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-zA-Z0-9]+");

    public interface Factory {
        CustomReader create(OpenAPIConfiguration openAPIConfig);
    }

    private final Map<Class<? extends PluginRestResource>, String> prefixes;

    @Inject
    public CustomReader(final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                        @Assisted OpenAPIConfiguration openAPIConfig) {
        super(openAPIConfig);
        this.prefixes = pluginRestResources.entrySet().stream().flatMap(entry -> {
            final var pluginId = entry.getKey();
            final var resources = entry.getValue();
            final var prefix = JerseyService.PLUGIN_PREFIX + "/" + pluginId;
            return resources.stream().map(resource -> Map.entry(resource, prefix));
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // We are synchronizing this method because the read process modifies the internal state of the Reader instance.
    @Override
    public synchronized OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        final var openAPI = super.read(classes, resources);

        // Remove all markers from paths that we've added in the read method below
        final var newPaths = openAPI.getPaths().entrySet()
                .stream()
                .collect(Collectors.toMap(e ->
                        StringUtils.defaultString(Strings.CS.removeStart(e.getKey(), PATH_MARKER)), Map.Entry::getValue));

        openAPI.getPaths().clear();
        openAPI.getPaths().putAll(newPaths);

        return openAPI;
    }

    /**
     * We are overriding the read method to add a prefix to the paths of plugin REST resources.
     * To make sure that we correctly preserve all paths even if a system resource and a plugin resource share the same
     * path we are adding a marker to the beginning of <em>all</em> paths. Otherwise, a plugin resource would override
     * the system resource path because we can only add the plugin prefix after the upstream reader has processed the
     * resource.
     */
    @Override
    public synchronized OpenAPI read(Class<?> cls, String parentPath, String parentMethod, boolean isSubresource, RequestBody parentRequestBody, ApiResponses parentResponses, Set<String> parentTags, List<Parameter> parentParameters, Set<Class<?>> scannedResources) {
        final var pathPrefix = PATH_MARKER + prefixes.getOrDefault(cls, "");

        final OpenAPI openAPI = super.read(cls, parentPath, parentMethod, isSubresource, parentRequestBody, parentResponses, parentTags, parentParameters, scannedResources);

        final Paths newPaths = new Paths();

        // Remove previously added paths that have not been handled and collect them to re-add with the correct prefix
        final var it = openAPI.getPaths().entrySet().iterator();
        while (it.hasNext()) {
            final var entry = it.next();
            final var path = entry.getKey();
            if (!path.startsWith(PATH_MARKER)) {
                final var newKey = Objects.requireNonNull(path).startsWith("/")
                        ? pathPrefix + path
                        : pathPrefix + "/" + path;
                newPaths.put(newKey, entry.getValue());
                it.remove();
            }
        }

        openAPI.getPaths().putAll(newPaths);
        return openAPI;
    }

    /**
     * Assigns a stable, derivation-based operationId by overriding the per-method parser.
     * <p>
     * swagger-core's stock behaviour falls back to {@code method.getName()} for unannotated
     * methods and disambiguates collisions by appending {@code _1}, {@code _2}, etc. That
     * makes the operationId of any given method depend on scan order, so adding or
     * reordering unrelated resources shifts every later operationId in the spec.
     * <p>
     * We let {@code super.parseMethod(...)} do its work as usual, then replace the
     * operationId on the returned Operation. For methods with a non-blank
     * {@code @Operation(operationId = "...")} annotation we restore the explicit value
     * verbatim — bypassing the {@code _N} dedup that would otherwise mangle deliberately
     * chosen operationIds. Otherwise, we derive the id from the declaring class name,
     * the method name, and the parameter names — see {@link #operationId(Method)}.
     */
    @Override
    protected Operation parseMethod(
            Class<?> cls,
            Method method,
            List<Parameter> globalParameters,
            Produces methodProduces,
            Produces classProduces,
            Consumes methodConsumes,
            Consumes classConsumes,
            List<SecurityRequirement> classSecurityRequirements,
            Optional<io.swagger.v3.oas.models.ExternalDocumentation> classExternalDocs,
            Set<String> classTags,
            List<io.swagger.v3.oas.models.servers.Server> classServers,
            boolean isSubresource,
            RequestBody parentRequestBody,
            ApiResponses parentResponses,
            JsonView jsonViewAnnotation,
            io.swagger.v3.oas.annotations.responses.ApiResponse[] classResponses,
            AnnotatedMethod annotatedMethod) {
        final Operation op = super.parseMethod(
                cls, method, globalParameters,
                methodProduces, classProduces, methodConsumes, classConsumes,
                classSecurityRequirements, classExternalDocs, classTags, classServers,
                isSubresource, parentRequestBody, parentResponses, jsonViewAnnotation,
                classResponses, annotatedMethod);

        if (op == null) {
            return null;
        }

        final var annotation = ReflectionUtils.getAnnotation(method, io.swagger.v3.oas.annotations.Operation.class);
        if (annotation != null && !annotation.operationId().isEmpty()) {
            op.setOperationId(annotation.operationId());
        } else {
            op.setOperationId(operationId(method));
        }
        return op;
    }

    /**
     * Short-circuits swagger-core's collision-deduplication walk.
     * <p>
     * The upstream implementation walks the in-memory paths map (O(n²) overall) to
     * append {@code _1}, {@code _2}, … on collisions. Our {@link #parseMethod} override
     * always overwrites the operationId after {@code super.parseMethod(...)} returns,
     * so that work is wasted. Returning the seed unchanged saves the traversals and
     * makes the relationship between the two overrides explicit: this class owns
     * operationId assignment, swagger-core's dedup logic does not run.
     */
    @Override
    protected String getOperationId(String operationId) {
        return operationId;
    }

    /**
     * Returns the derived operationId for a JAX-RS handler method.
     * <p>
     * Shape: {@code [<PluginPrefix>_]<ClassWithoutResource>_<methodName>[By<P1>And<P2>…]}.
     * Plugin classes (those whose package contains {@code plugins.<id>.…}) get the
     * capitalized {@code <id>} as a prefix; core classes get no prefix. The class name
     * has any trailing {@code Resource} stripped. Methods with JAX-RS parameters always
     * get a {@code By…} suffix, so the id stays stable if an overload is added later
     * (no flip-flop between bare and suffixed forms).
     * <p>
     * Parameter names are sourced from JAX-RS parameter annotations
     * ({@link PathParam}, {@link QueryParam}, {@link HeaderParam}, {@link CookieParam},
     * {@link FormParam}, {@link MatrixParam}). Body parameters and {@code @Context}
     * injections are ignored. {@code @BeanParam} composites are not recursed into.
     * <p>
     * The class part is taken from {@code method.getDeclaringClass()}. Handler methods
     * inherited from a shared base class therefore derive the <em>same</em> id in every
     * subclass that exposes them — a collision. We accept this because no production
     * resource currently inherits handlers, and the spec validation will tell us;
     * the fix is an explicit {@code @Operation(operationId = "...")} on the affected methods.
     */
    static String operationId(Method method) {
        final var declaringClass = method.getDeclaringClass();
        final var pluginPrefix = derivePluginPrefix(declaringClass.getPackage().getName());
        final var classPart = stripResourceSuffix(declaringClass.getSimpleName());
        final var base = Stream.of(pluginPrefix, classPart, method.getName())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("_"));
        return base + paramSuffix(extractParamNames(method));
    }

    /**
     * Returns the names of JAX-RS-annotated parameters on {@code method}, in declaration order.
     */
    static List<String> extractParamNames(Method method) {
        return Arrays.stream(method.getParameters())
                .map(CustomReader::jaxRsParamName)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String jaxRsParamName(java.lang.reflect.Parameter p) {
        if (p.getAnnotation(PathParam.class) instanceof PathParam a) return a.value();
        if (p.getAnnotation(QueryParam.class) instanceof QueryParam a) return a.value();
        if (p.getAnnotation(HeaderParam.class) instanceof HeaderParam a) return a.value();
        if (p.getAnnotation(CookieParam.class) instanceof CookieParam a) return a.value();
        if (p.getAnnotation(FormParam.class) instanceof FormParam a) return a.value();
        if (p.getAnnotation(MatrixParam.class) instanceof MatrixParam a) return a.value();
        return null;
    }

    /**
     * Returns the capitalized first segment that follows {@code plugins} in the package
     * name, or empty for packages that don't contain {@code plugins}. E.g. {@code
     * org.graylog.plugins.sidecar.rest.resources} → {@code Sidecar}.
     */
    static String derivePluginPrefix(String packageName) {
        final var segments = packageName.split("\\.");
        for (int i = 0; i < segments.length - 1; i++) {
            if (segments[i].equals("plugins")) {
                return StringUtils.capitalize(segments[i + 1]);
            }
        }
        return "";
    }

    /**
     * Strips a trailing {@code Resource} from a class's simple name. Names that don't
     * end in {@code Resource} are returned unchanged.
     */
    static String stripResourceSuffix(String simpleName) {
        return Strings.CS.removeEnd(simpleName, "Resource");
    }

    /**
     * Returns {@code By<P1>And<P2>…} for non-empty parameter lists, empty string otherwise.
     * Parameter names are normalized via {@link #normalizeParamName(String)} and joined
     * in JAX-RS-method declaration order (not sorted).
     */
    static String paramSuffix(List<String> paramNames) {
        if (paramNames == null || paramNames.isEmpty()) {
            return "";
        }
        return "By" + paramNames.stream()
                .map(CustomReader::normalizeParamName)
                .collect(Collectors.joining("And"));
    }

    /**
     * Converts a parameter name into a CamelCase identifier suitable for embedding in
     * an operationId. Splits on any non-alphanumeric run, capitalizes the first letter
     * of each segment, leaves internal capitals intact (so {@code XMLContent} stays
     * {@code XMLContent}, not {@code Xmlcontent}).
     */
    static String normalizeParamName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return NON_ALPHANUM.splitAsStream(name)
                .filter(s -> !s.isEmpty())
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }
}
