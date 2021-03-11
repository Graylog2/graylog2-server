package org.graylog.plugins.views.search.rest;

import org.graylog.plugins.views.search.export.ExportFormat;
import org.graylog2.rest.MoreMediaTypes;

import javax.inject.Inject;
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
import java.util.stream.Collectors;

@SupportOnlyValidExportFormats
@PreMatching
public class MessageExportFormatFilter implements ContainerRequestFilter {
    private final Map<MediaType, ExportFormat> supportedFormats;

    @Inject
    public MessageExportFormatFilter(Set<ExportFormat> supportedFormats) {
        this.supportedFormats = supportedFormats.stream()
            .collect(Collectors.toMap(ExportFormat::mimeType, Function.identity()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final List<MediaType> acceptedFormats = requestContext.getAcceptableMediaTypes();

        final Map<MediaType, ExportFormat> exportFormatCandidates = supportedFormats.entrySet()
                .stream()
                .filter(entry -> acceptedFormats.stream().anyMatch(acceptedFormat -> entry.getKey().isCompatible(acceptedFormat)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (exportFormatCandidates.isEmpty()) {
            requestContext.abortWith(Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build());
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
        }

        final List<String> allowedMediaTypes = candidateErrors.entrySet().stream()
                .filter(entry -> !entry.getValue().isPresent())
                .map(Map.Entry::getKey)
                .map(MediaType::toString)
                .collect(Collectors.toList());

        requestContext.getHeaders().put(HttpHeaders.ACCEPT, allowedMediaTypes);
    }
}
