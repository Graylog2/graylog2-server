package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.http.HttpHeaders;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.graylog.plugins.views.search.export.ExportFormat;
import org.graylog2.rest.MoreMediaTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageExportFormatFilterTest {
    private static final String VALID_PATH = "/views/search/messages";
    private static final ExportFormat disabledJsonExportFormat = new ExportFormat() {
        @Override
        public MediaType mimeType() {
            return MoreMediaTypes.APPLICATION_JSON_TYPE;
        }

        @Override
        public Optional<String> hasError() {
            return Optional.of("Kaboom!");
        }
    };

    private ArgumentCaptor<Response> responseCaptor;

    @BeforeEach
    void setUp() {
        this.responseCaptor = ArgumentCaptor.forClass(Response.class);
    }

    @Test
    void returns415IfNoFormatIsPresent() throws Exception {
        final ContainerRequestFilter filter = new MessageExportFormatFilter(Collections.emptySet());
        final ContainerRequestContext requestContext = mockRequestContext();

        filter.filter(requestContext);

        verifyRequestAborted(requestContext);
    }

    @Test
    void returns415IfNoCompatibleFormatIsFound() throws Exception {
        final ContainerRequestFilter filter = new MessageExportFormatFilter(Collections.singleton(() -> MoreMediaTypes.TEXT_PLAIN_TYPE));
        final ContainerRequestContext requestContext = mockRequestContext(Collections.singletonList(MoreMediaTypes.APPLICATION_JSON_TYPE));

        filter.filter(requestContext);

        verifyRequestAborted(requestContext);
    }

    @Test
    void returns415IfNoAcceptedFormatIsSpecified() throws Exception {
        final ContainerRequestFilter filter = new MessageExportFormatFilter(Collections.singleton(() -> MoreMediaTypes.TEXT_PLAIN_TYPE));
        final ContainerRequestContext requestContext = mockRequestContext(Collections.emptyList());

        filter.filter(requestContext);

        verifyRequestAborted(requestContext);
    }

    @Test
    void returns415IfAcceptedFormatIsNotEnabled() throws Exception {
        final ContainerRequestFilter filter = new MessageExportFormatFilter(Collections.singleton(disabledJsonExportFormat));
        final ContainerRequestContext requestContext = mockRequestContext(Collections.emptyList());

        filter.filter(requestContext);

        verifyRequestAborted(requestContext);
    }

    @Test
    void doesNothingIfRequestPathDoesNotMatch() throws Exception {
        final ContainerRequestFilter filter = new MessageExportFormatFilter(Collections.emptySet());
        final ContainerRequestContext requestContext = mockRequestContextForNonMatchingPath();

        filter.filter(requestContext);

        verifyRequestNotAborted(requestContext);
    }

    @Test
    void filtersAcceptedMediaTypesToExistingOnes() throws Exception {
        final ExportFormat jsonExportFormat = () -> MoreMediaTypes.APPLICATION_JSON_TYPE;
        final ExportFormat plainTextExportFormat = () -> MoreMediaTypes.TEXT_PLAIN_TYPE;

        final ContainerRequestFilter filter = new MessageExportFormatFilter(ImmutableSet.of(jsonExportFormat, plainTextExportFormat));
        final ContainerRequestContext requestContext = mockRequestContext(ImmutableList.of(
                MoreMediaTypes.TEXT_CSV_TYPE,
                MoreMediaTypes.TEXT_PLAIN_TYPE,
                MoreMediaTypes.APPLICATION_JSON_TYPE
        ));

        filter.filter(requestContext);

        verifyRequestNotAborted(requestContext);
        assertThat(requestContext.getHeaders().get(HttpHeaders.ACCEPT)).containsExactly(MoreMediaTypes.APPLICATION_JSON, MoreMediaTypes.TEXT_PLAIN);
    }

    @Test
    void filtersAcceptedMediaTypesToEnabledOnes() throws Exception {
        final ExportFormat plainTextExportFormat = () -> MoreMediaTypes.TEXT_PLAIN_TYPE;

        final ContainerRequestFilter filter = new MessageExportFormatFilter(ImmutableSet.of(disabledJsonExportFormat, plainTextExportFormat));
        final ContainerRequestContext requestContext = mockRequestContext(ImmutableList.of(
                MoreMediaTypes.TEXT_CSV_TYPE,
                MoreMediaTypes.TEXT_PLAIN_TYPE,
                MoreMediaTypes.APPLICATION_JSON_TYPE
        ));

        filter.filter(requestContext);

        verifyRequestNotAborted(requestContext);
        assertThat(requestContext.getHeaders().get(HttpHeaders.ACCEPT)).containsExactly(MoreMediaTypes.TEXT_PLAIN);
    }

    private void verifyRequestNotAborted(ContainerRequestContext requestContext) {
        verify(requestContext, never()).abortWith(any());
    }

    private void verifyRequestAborted(ContainerRequestContext requestContext) {
        verify(requestContext, times(1)).abortWith(responseCaptor.capture());

        assertThat(responseCaptor.getValue().getStatusInfo())
                .isEqualTo(Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }

    private ContainerRequestContext mockRequestContextForNonMatchingPath() {
        final UriInfo uriInfo = uriInfo("/api/system/inputs");

        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        return requestContext;
    }

    private ContainerRequestContext mockRequestContext(List<MediaType> acceptedMediaTypes) {
        final UriInfo uriInfo = uriInfo(VALID_PATH);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(acceptedMediaTypes);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);

        final MultivaluedMap<String, String> headers = new MultivaluedStringMap(1);
        headers.put(HttpHeaders.ACCEPT, acceptedMediaTypes.stream().map(MediaType::toString).collect(Collectors.toList()));
        when(requestContext.getHeaders()).thenReturn(headers);

        return requestContext;
    }

    private UriInfo uriInfo(String path) {
        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);

        return uriInfo;
    }

    private ContainerRequestContext mockRequestContext() {
        return mockRequestContext(Collections.emptyList());
    }
}
