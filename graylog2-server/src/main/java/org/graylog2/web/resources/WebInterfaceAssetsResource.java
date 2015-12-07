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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Plugin;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.ModuleManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@Path("{filename: .*}")
public class WebInterfaceAssetsResource {
    private static final Logger LOG = LoggerFactory.getLogger(WebInterfaceAssetsResource.class);

    private static String pathPrefix = "web-interface/assets";
    private static String pluginPathPrefix = "/plugin/";
    private static String manifestFilename = "module.json";
    private final IndexHtmlGenerator indexHtmlGenerator;
    private final ObjectMapper objectMapper;

    @Inject
    public WebInterfaceAssetsResource(ObjectMapper objectMapper,
                                      Set<Plugin> plugins) throws IOException {
        this.objectMapper = objectMapper;
        final List<String> jsFiles = new ArrayList<>();
        final List<String> cssFiles = new ArrayList<>();

        plugins.stream().forEach(plugin -> {
            final ModuleManifest pluginManifest = manifestForPlugin(plugin);
            final String pathPrefix = pluginPathPrefix + plugin.metadata().getUniqueId() + "/";
            if (pluginManifest != null) {
                jsFiles.addAll(pluginManifest.files().jsFiles().stream().map(file -> file.startsWith(pathPrefix) ? file : pathPrefix + file).collect(Collectors.toList()));
                cssFiles.addAll(pluginManifest.files().cssFiles().stream().map(file -> file.startsWith(pathPrefix) ? file : pathPrefix + file).collect(Collectors.toList()));
            }
        });
        final InputStream packageManifest = ClassLoader.getSystemResourceAsStream(pathPrefix + "/" + manifestFilename);
        if (packageManifest != null) {
            final ModuleManifest manifest = objectMapper.readValue(packageManifest, ModuleManifest.class);
            jsFiles.addAll(manifest.files().jsFiles());
            cssFiles.addAll(manifest.files().cssFiles());
        } else {
            LOG.warn("Unable to find web interface assets. Maybe the web interface was not built into server?");
        }
        this.indexHtmlGenerator = new IndexHtmlGenerator("Graylog Web Interface", cssFiles, jsFiles);
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

    private ModuleManifest manifestForPlugin(Plugin plugin) {
        final InputStream manifestStream = plugin.metadata().getClass().getResourceAsStream("/" + manifestFilename);
        if (manifestStream != null) {
            try {
                final ModuleManifest manifest = objectMapper.readValue(manifestStream, ModuleManifest.class);
                return manifest;
            } catch (IOException e) {
                LOG.warn("Unable to read manifest from plugin " + plugin + ": ", e);
            }
        }

        LOG.debug("No valid manifest found for plugin " + plugin);

        return null;
    }

    private static InputStream getStreamForFile(String filename) {
        return ClassLoader.getSystemResourceAsStream(pathPrefix + "/" + filename);
    }

    private Response getDefaultResponse() {
        return Response
                .ok(this.indexHtmlGenerator.get())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .build();
    }
}
