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
package org.graylog2.web.resources;

import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.PluginAssets;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URLConnection;

import static com.google.common.base.MoreObjects.firstNonNull;

@Path("{filename: .*}")
public class WebInterfaceAssetsResource {
    private final IndexHtmlGenerator indexHtmlGenerator;

    @Inject
    public WebInterfaceAssetsResource(IndexHtmlGenerator indexHtmlGenerator) {
        this.indexHtmlGenerator = indexHtmlGenerator;
    }

    @GET
    public Response get(@PathParam("filename") String filename) {
        if (filename == null || filename.isEmpty() || filename.equals("/") || filename.equals("index.html")) {
            return getDefaultResponse();
        }
        final InputStream stream = getStreamForFile(filename);
        if (stream == null) {
            return getDefaultResponse();
        }

        final String contentType = firstNonNull(URLConnection.guessContentTypeFromName(filename), MediaType.APPLICATION_OCTET_STREAM);
        return Response
                .ok(stream)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .build();
    }

    private InputStream getStreamForFile(String filename) {
        return this.getClass().getResourceAsStream("/" + PluginAssets.pathPrefix + "/" + filename);
    }

    private Response getDefaultResponse() {
        return Response
                .ok(this.indexHtmlGenerator.get())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .build();
    }
}
