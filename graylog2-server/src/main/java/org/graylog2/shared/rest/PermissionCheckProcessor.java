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

import com.google.common.collect.ComparisonChain;
import jakarta.ws.rs.core.Configuration;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * A Jersey {@link ModelProcessor} that checks each resource method for authentication annotations.
 * <p>
 * It <b>stops the JVM process</b> if there are any resource methods which are not protected by authentication. This is a
 * security measure to avoid exposing unauthenticated resource endpoints by accident.
 * <p>
 * Endpoints which are supposed to be accessible without authentication should be annotated with the
 * {@link NoPermissionCheckRequired} annotation.
 */
public class PermissionCheckProcessor implements ModelProcessor {
    private final Logger LOG = LoggerFactory.getLogger(PermissionCheckProcessor.class);

    @Override
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
        checkResources(resourceModel.getResources());
        return resourceModel;
    }

    @Override
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration) {
        checkResources(subResourceModel.getResources());
        return subResourceModel;
    }

    private void checkResources(List<Resource> resources) {
        final List<UnprotectedEndpoint> unprotectedEndpoints = new ArrayList<>();

        for (final Resource resource : resources) {
            unprotectedEndpoints.addAll(checkResource(resource));
        }

        if (!unprotectedEndpoints.isEmpty()) {
            StringBuilder sb = new StringBuilder("\n");
            sb.append("================================================================================\n");
            sb.append("    ").append(unprotectedEndpoints.size()).append("         UNPROTECTED API ENDPOINTS!\n");
            sb.append("================================================================================\n");
            sb.append("\n");
            unprotectedEndpoints.stream().sorted().forEach(endpoint -> sb.append("  endpoint: ").append(endpoint).append("\n"));
            sb.append("\n");
            sb.append("Use @PublicResource or @ManualPermissionCheck on the method if this is intended.\n");
            sb.append("================================================================================\n");
            LOG.error(sb.toString());
        }
    }

    private List<UnprotectedEndpoint> checkResource(Resource resource) {
        final List<UnprotectedEndpoint> unprotectedEndpoints = new ArrayList<>();

        for (final ResourceMethod method : resource.getResourceMethods()) {
            final Method definitionMethod = method.getInvocable().getDefinitionMethod();
            if (definitionMethod.isAnnotationPresent(NoPermissionCheckRequired.class) || definitionMethod.isAnnotationPresent(InlinePermissionCheck.class)) {
                // skip further checks
                continue;
            }
            Stream.concat(Arrays.stream(definitionMethod.getAnnotations()), Arrays.stream(definitionMethod.getDeclaringClass().getAnnotations()))
                    .filter(annotation -> annotation.annotationType().equals(RequiresPermissions.class))
                    .findFirst()
                    .ifPresentOrElse(annotation -> {
                        if (annotation instanceof RequiresPermissions p) {
                            if (Arrays.equals(p.value(), new String[]{"*"})) {
                                LOG.error("Do not use wildcard permissions in resource annotations: {}", new UnprotectedEndpoint(method, resource));
                            }
                        }
                    }, () -> unprotectedEndpoints.add(new UnprotectedEndpoint(method, resource)));
        }

        for (final Resource childResource : resource.getChildResources()) {
            unprotectedEndpoints.addAll(checkResource(childResource));
        }

        return unprotectedEndpoints;
    }


    private static class UnprotectedEndpoint implements Comparable<UnprotectedEndpoint> {
        final String httpMethod;
        final String resourcePath;
        final String methodRef;

        UnprotectedEndpoint(ResourceMethod method, Resource resource) {
            this.httpMethod = method.getHttpMethod();
            this.resourcePath = getPathFromResource(resource);
            final Method definitionMethod = method.getInvocable().getDefinitionMethod();
            this.methodRef = definitionMethod.getDeclaringClass().getCanonicalName() + "#" + definitionMethod.getName();
        }

        private String getPathFromResource(Resource resource) {
            String path = resource.getPath();
            Resource parent = resource.getParent();

            while (parent != null) {
                if (!path.startsWith("/")) {
                    //noinspection StringConcatenationInLoop
                    path = "/" + path;
                }

                //noinspection StringConcatenationInLoop
                path = parent.getPath() + path;
                parent = parent.getParent();
            }

            return path;

        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%6s %-80s %-120s", httpMethod, resourcePath, methodRef);
        }

        @Override
        public int compareTo(@NotNull PermissionCheckProcessor.UnprotectedEndpoint o) {
            return ComparisonChain.start()
                    .compare(resourcePath, o.resourcePath)
                    .compare(httpMethod, o.httpMethod)
                    .compare(methodRef, o.methodRef)
                    .result();
        }
    }
}
