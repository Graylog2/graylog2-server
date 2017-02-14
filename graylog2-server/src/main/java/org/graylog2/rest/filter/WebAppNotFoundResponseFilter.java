/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.filter;

import org.graylog2.Configuration;
import org.graylog2.web.IndexHtmlGenerator;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

@Priority(Priorities.ENTITY_CODER)
public class WebAppNotFoundResponseFilter implements ContainerResponseFilter {
    private final String webAppPrefix;
    private final IndexHtmlGenerator indexHtmlGenerator;

    @Inject
    public WebAppNotFoundResponseFilter(Configuration configuration, IndexHtmlGenerator indexHtmlGenerator) {
        this.webAppPrefix = configuration.getWebListenUri().getPath();
        this.indexHtmlGenerator = indexHtmlGenerator;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final Response.StatusType responseStatus = responseContext.getStatusInfo();
        final String requestPath = requestContext.getUriInfo().getAbsolutePath().getPath();
        final List<MediaType> acceptableMediaTypes = requestContext.getAcceptableMediaTypes();
        final boolean acceptsHtml = acceptableMediaTypes.stream()
                .anyMatch(mediaType -> mediaType.isCompatible(MediaType.TEXT_HTML_TYPE) || mediaType.isCompatible(MediaType.APPLICATION_XHTML_XML_TYPE));
        final boolean isGetRequest = "get".equalsIgnoreCase(requestContext.getMethod());

        if (isGetRequest
                && responseStatus == Response.Status.NOT_FOUND
                && acceptsHtml
                && requestPath.startsWith(webAppPrefix)) {
            final String entity = indexHtmlGenerator.get();
            responseContext.setStatusInfo(Response.Status.OK);
            responseContext.setEntity(entity, new Annotation[0], MediaType.TEXT_HTML_TYPE);

            responseContext.getHeaders().putSingle("X-UA-Compatible", "IE=edge");
        }
    }
}
