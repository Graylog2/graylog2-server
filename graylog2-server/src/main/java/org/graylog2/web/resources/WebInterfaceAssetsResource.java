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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.plugin.Plugin;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.PluginAssets;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Objects.requireNonNull;

@Singleton
@Path("")
public class WebInterfaceAssetsResource {
    private final MimetypesFileTypeMap mimeTypes;
    private final IndexHtmlGenerator indexHtmlGenerator;
    private final Set<Plugin> plugins;
    private final LoadingCache<URI, FileSystem> fileSystemCache;

    @Inject
    public WebInterfaceAssetsResource(IndexHtmlGenerator indexHtmlGenerator, Set<Plugin> plugins, MimetypesFileTypeMap mimeTypes) {
        this.indexHtmlGenerator = indexHtmlGenerator;
        this.plugins = plugins;
        this.mimeTypes = requireNonNull(mimeTypes);
        fileSystemCache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .build(new CacheLoader<URI, FileSystem>() {
                    @Override
                    public FileSystem load(@Nonnull URI key) throws Exception {
                        try {
                            return FileSystems.getFileSystem(key);
                        } catch (FileSystemNotFoundException e) {
                            try {
                                return FileSystems.newFileSystem(key, Collections.emptyMap());
                            } catch (FileSystemAlreadyExistsException f) {
                                return FileSystems.getFileSystem(key);
                            }
                        }
                    }
                });
    }

    @Path("plugin/{plugin}/{filename}")
    @GET
    public Response get(@Context Request request,
                        @PathParam("plugin") String pluginName,
                        @PathParam("filename") String filename) {
        final Optional<Plugin> plugin = getPluginForName(pluginName);
        if (!plugin.isPresent()) {
            throw new NotFoundException("Couldn't find plugin " + pluginName);
        }

        try {
            final URL resourceUrl = getResourceUri(true, filename, plugin.get().metadata().getClass());
            return getResponse(request, filename, resourceUrl, true);
        } catch (URISyntaxException | IOException e) {
            throw new NotFoundException("Couldn't find " + filename + " in plugin " + pluginName, e);
        }
    }

    private Optional<Plugin> getPluginForName(String pluginName) {
        return this.plugins.stream().filter(plugin -> plugin.metadata().getUniqueId().equals(pluginName)).findFirst();
    }

    @Path("{filename: .*}")
    @GET
    public Response get(@Context Request request,
                        @PathParam("filename") String filename) {
        if (filename == null || filename.isEmpty() || filename.equals("/") || filename.equals("index.html")) {
            return getDefaultResponse();
        }
        try {
            final URL resourceUrl = getResourceUri(false, filename, this.getClass());
            return getResponse(request, filename, resourceUrl, false);
        } catch (IOException | URISyntaxException e) {
            return getDefaultResponse();
        }
    }

    @GET
    public Response getIndex(@Context ContainerRequest request) {
        final URI originalLocation = request.getRequestUri();
        if (originalLocation.getPath().endsWith("/")) {
            return get(request, originalLocation.getPath());
        }
        final URI redirect = UriBuilder.fromPath(originalLocation.getPath() + "/").build();
        return Response.temporaryRedirect(redirect).build();
    }

    private Response getResponse(Request request, String filename,
                                 URL resourceUrl, boolean fromPlugin) throws IOException, URISyntaxException {
        final Date lastModified;
        final InputStream stream;
        final HashCode hashCode;

        switch (resourceUrl.getProtocol()) {
            case "file": {
                final File file = new File(resourceUrl.toURI());
                lastModified = new Date(file.lastModified());
                stream = new FileInputStream(file);
                hashCode = Files.hash(file, Hashing.sha256());
                break;
            }
            case "jar": {
                final URI uri = resourceUrl.toURI();
                final FileSystem fileSystem = fileSystemCache.getUnchecked(uri);
                final java.nio.file.Path path = fileSystem.getPath(pluginPrefixFilename(fromPlugin,
                                                                                        filename
                ));
                final FileTime lastModifiedTime = java.nio.file.Files.getLastModifiedTime(path);
                lastModified = new Date(lastModifiedTime.toMillis());
                stream = resourceUrl.openStream();
                hashCode = Resources.asByteSource(resourceUrl).hash(Hashing.sha256());
                break;
            }
            default:
                throw new IllegalArgumentException("Not a jar or file");
        }

        final EntityTag entityTag = new EntityTag(hashCode.toString());

        final Response.ResponseBuilder response = request.evaluatePreconditions(lastModified, entityTag);
        if (response != null) {
            return response.build();
        }

        final String contentType = firstNonNull(mimeTypes.getContentType(filename),
                                                MediaType.APPLICATION_OCTET_STREAM);
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) TimeUnit.DAYS.toSeconds(365));
        cacheControl.setNoCache(false);
        cacheControl.setPrivate(false);
        return Response
                .ok(stream)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .tag(entityTag)
                .cacheControl(cacheControl)
                .lastModified(lastModified)
                .build();
    }

    private URL getResourceUri(boolean fromPlugin, String filename,
                               Class<?> aClass) throws URISyntaxException, FileNotFoundException {
        final URL resourceUrl = aClass.getResource(pluginPrefixFilename(fromPlugin, filename));
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource file " + filename + " not found.");
        }
        return resourceUrl;
    }

    @Nonnull
    private String pluginPrefixFilename(boolean fromPlugin, String filename) {
        if (fromPlugin) {
            return "/" + filename;
        } else {
            return "/" + PluginAssets.pathPrefix + "/" + filename;
        }
    }

    private Response getDefaultResponse() {
        return Response
                .ok(this.indexHtmlGenerator.get())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .header("X-UA-Compatible", "IE=edge")
                .build();
    }
}
