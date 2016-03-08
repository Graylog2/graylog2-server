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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.PluginAssets;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

@Path("{filename: .*}")
public class WebInterfaceAssetsResource {
    private final IndexHtmlGenerator indexHtmlGenerator;

    @Inject
    public WebInterfaceAssetsResource(IndexHtmlGenerator indexHtmlGenerator) {
        this.indexHtmlGenerator = indexHtmlGenerator;
    }

    @GET
    public Response get(@Context Request request, @Context HttpHeaders httpheaders, @PathParam("filename") String filename) {
        if (filename == null || filename.isEmpty() || filename.equals("/") || filename.equals("index.html")) {
            return getDefaultResponse();
        }
        try {
            final File resourceFile = getResourceFile(filename);
            final InputStream stream = new FileInputStream(resourceFile);
            final HashCode hashCode = Files.hash(resourceFile, Hashing.sha256());
            final EntityTag entityTag = new EntityTag(hashCode.toString());
            final Date lastModified = new Date(resourceFile.lastModified());

            final Response.ResponseBuilder response = request.evaluatePreconditions(lastModified, entityTag);
            if (response != null) {
                return response.build();
            }

            final String contentType = firstNonNull(URLConnection.guessContentTypeFromName(filename), MediaType.APPLICATION_OCTET_STREAM);
            final CacheControl cacheControl = new CacheControl();
            cacheControl.setMaxAge((int)TimeUnit.DAYS.toSeconds(365));
            cacheControl.setNoCache(false);
            cacheControl.setPrivate(false);
            return Response
                .ok(stream)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .tag(entityTag)
                .cacheControl(cacheControl)
                .lastModified(lastModified)
                .build();
        } catch (IOException | URISyntaxException e) {
            return getDefaultResponse();
        }
    }

    private File getResourceFile(String filename) throws URISyntaxException, FileNotFoundException {
        final URL resourceUrl =  this.getClass().getResource("/" + PluginAssets.pathPrefix + "/" + filename);
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource file " + filename + " not found.");
        }
        return new File(resourceUrl.toURI());
    }

    private Response getDefaultResponse() {
        return Response
                .ok(this.indexHtmlGenerator.get())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .header("X-UA-Compatible", "IE=edge")
                .build();
    }
}
