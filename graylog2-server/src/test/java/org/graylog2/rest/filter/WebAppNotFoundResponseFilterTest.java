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
package org.graylog2.rest.filter;

import org.graylog2.web.IndexHtmlGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebAppNotFoundResponseFilterTest {
    private static final String CK_METHOD_GET = "GET";
    private static final String CK_METHOD_POST = "POST";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private ContainerResponseContext responseContext;
    @Mock
    private IndexHtmlGenerator indexHtmlGenerator;

    private WebAppNotFoundResponseFilter filter;
    private MultivaluedHashMap<String, Object> responseHeaders;

    @Before
    public void setUp() throws Exception {
        responseHeaders = new MultivaluedHashMap<>();
        when(indexHtmlGenerator.get(any())).thenReturn("index.html");
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        filter = new WebAppNotFoundResponseFilter(indexHtmlGenerator);
    }

    @Test
    public void filterDoesNotFilterApplicationJson() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void filterDoesFilterTextHtml() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.TEXT_HTML_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        verify(responseContext, times(1)).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void filterDoesFilterApplicationXhtml() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.APPLICATION_XHTML_XML_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        verify(responseContext, times(1)).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void filterDoesFilterCompatibleAcceptMimeTypes() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.WILDCARD_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        verify(responseContext, times(1)).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void filterDoesNotFilterRestApiPrefix() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.TEXT_HTML_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/api/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void filterDoesNotFilterResponseStatusOk() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.TEXT_HTML_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.OK);

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }

    @Test
    public void filterAddsUserAgentResponseHeader() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.TEXT_HTML_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/search"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_GET);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        assertThat(responseHeaders).containsEntry("X-UA-Compatible", Collections.singletonList("IE=edge"));
    }

    @Test
    public void filterDoesNotFilterPostRequests() throws Exception {
        final UriInfo uriInfo = mock(UriInfo.class);
        final List<MediaType> mediaTypes = Collections.singletonList(MediaType.TEXT_HTML_TYPE);
        when(uriInfo.getAbsolutePath()).thenReturn(URI.create("/web/nonexisting"));
        when(requestContext.getMethod()).thenReturn(CK_METHOD_POST);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getAcceptableMediaTypes()).thenReturn(mediaTypes);
        when(responseContext.getStatusInfo()).thenReturn(Response.Status.NOT_FOUND);

        filter.filter(requestContext, responseContext);

        verify(responseContext, never()).setEntity("index.html", new Annotation[0], MediaType.TEXT_HTML_TYPE);
    }
}
