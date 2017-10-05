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
package org.graylog2.shared.rest.resources.documentation;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.RestResource;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Path("/api-browser")
public class DocumentationBrowserResource extends RestResource {
    private final MimetypesFileTypeMap mimeTypes;
    private final HttpConfiguration httpConfiguration;

    private final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    private final Engine templateEngine;

    @Inject
    public DocumentationBrowserResource(MimetypesFileTypeMap mimeTypes, HttpConfiguration httpConfiguration, Engine templateEngine) {
        this.mimeTypes = requireNonNull(mimeTypes, "mimeTypes");
        this.httpConfiguration = requireNonNull(httpConfiguration, "httpConfiguration");
        this.templateEngine = requireNonNull(templateEngine, "templateEngine");
    }

    @GET
    public Response root(@Context HttpHeaders httpHeaders) throws IOException {
        final String index = index(httpHeaders);
        return Response.ok(index, MediaType.TEXT_HTML_TYPE)
                .header(HttpHeaders.CONTENT_LENGTH, index.length())
                .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("index.html")
    public String index(@Context HttpHeaders httpHeaders) throws IOException {
        final URL templateUrl = this.getClass().getResource("/swagger/index.html.template");
        final String template = Resources.toString(templateUrl, StandardCharsets.UTF_8);
        final Map<String, Object> model = ImmutableMap.of(
                "baseUri", RestTools.buildExternalUri(httpHeaders.getRequestHeaders(), httpConfiguration.getHttpExternalUri()).resolve(HttpConfiguration.PATH_API).toString());
        return templateEngine.transform(template, model);
    }

    @GET
    @Path("/{route: .*}")
    public Response asset(@PathParam("route") String route) throws IOException {
        // Directory traversal should not be possible but just to make sure..
        if (route.contains("..")) {
            throw new BadRequestException("Not allowed to access parent directory");
        }

        final URL resource = classLoader.getResource("swagger/" + route);
        if (null != resource) {
            try {
                final byte[] resourceBytes = Resources.toByteArray(resource);

                return Response.ok(resourceBytes, mimeTypes.getContentType(route))
                        .header("Content-Length", resourceBytes.length)
                        .build();
            } catch (IOException e) {
                throw new NotFoundException("Couldn't load " + resource, e);
            }
        } else {
            throw new NotFoundException("Couldn't find " + route);
        }
    }
}
