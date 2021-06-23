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
package org.graylog.plugins.views.search.rest;

import org.graylog.plugins.views.search.export.ExportFormat;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.MoreMediaTypes;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@PreMatching
public class MessageExportFormatFilter implements ContainerRequestFilter {
    private static final Pattern PATTERN = Pattern.compile("^/");

    private final Map<MediaType, ExportFormat> supportedFormats;
    private final String targetPath;

    @Inject
    public MessageExportFormatFilter(Set<ExportFormat> supportedFormats) {
        this.supportedFormats = supportedFormats.stream()
                .collect(Collectors.toMap(ExportFormat::mimeType, Function.identity()));
        targetPath = HttpConfiguration.PATH_API + PATTERN.matcher(MessagesResource.class.getAnnotation(Path.class).value())
                .replaceFirst(""); // Remove leading "/" because PATH_API already includes it
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!requestContext.getUriInfo().getPath().endsWith(targetPath)) {
            return;
        }
        final List<MediaType> acceptedFormats = requestContext.getAcceptableMediaTypes();

        final Map<MediaType, ExportFormat> exportFormatCandidates = supportedFormats.entrySet()
                .stream()
                .filter(entry -> acceptedFormats.stream().anyMatch(acceptedFormat -> entry.getKey().isCompatible(acceptedFormat)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (exportFormatCandidates.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
            return;
        }

        final Map<MediaType, Optional<String>> candidateErrors = exportFormatCandidates.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().hasError()));

        if (candidateErrors.values().stream().allMatch(Optional::isPresent)) {
            final String errorMessage = candidateErrors.values().stream()
                    .map(optionalError -> optionalError.orElse(""))
                    .collect(Collectors.joining("\n"));
            requestContext.abortWith(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity(errorMessage)
                    .type(MoreMediaTypes.TEXT_PLAIN_TYPE)
                    .build());
            return;
        }

        final List<String> allowedMediaTypes = candidateErrors.entrySet().stream()
                .filter(entry -> !entry.getValue().isPresent())
                .map(Map.Entry::getKey)
                .map(MediaType::toString)
                .collect(Collectors.toList());

        requestContext.getHeaders().put(HttpHeaders.ACCEPT, allowedMediaTypes);
    }
}
