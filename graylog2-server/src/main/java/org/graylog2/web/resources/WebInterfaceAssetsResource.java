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
package org.graylog2.web.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.csp.CSP;
import org.graylog2.shared.rest.resources.csp.CSPDynamicFeature;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.PluginAssets;
import org.graylog2.web.customization.CustomizationConfig;
import org.jooq.lambda.tuple.Tuple2;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
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
@CSP(group = CSP.DEFAULT)
public class WebInterfaceAssetsResource {
    private static final String ASSETS_PREFIX = "assets";
    private static final String FAVICON = "favicon.png";
    private final MimetypesFileTypeMap mimeTypes;
    private final HttpConfiguration httpConfiguration;
    private final CustomizationConfig customizationConfig;
    private final IndexHtmlGenerator indexHtmlGenerator;
    private final Set<Plugin> plugins;
    private final LoadingCache<URI, FileSystem> fileSystemCache;

    @Inject
    public WebInterfaceAssetsResource(IndexHtmlGenerator indexHtmlGenerator,
                                      Set<Plugin> plugins,
                                      MimetypesFileTypeMap mimeTypes,
                                      HttpConfiguration httpConfiguration,
                                      CustomizationConfig customizationConfig) {
        this.indexHtmlGenerator = indexHtmlGenerator;
        this.plugins = plugins;
        this.mimeTypes = requireNonNull(mimeTypes);
        this.httpConfiguration = httpConfiguration;
        this.customizationConfig = customizationConfig;
        this.fileSystemCache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .build(new CacheLoader<>() {
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

    @Path(ASSETS_PREFIX + "/" + FAVICON)
    @GET
    public Response getFavicon(@Context ContainerRequest request) {
        final var fileContents = customizationConfig.favicon()
                .or(() -> {
                    try {
                        return Optional.of(readFile(false, FAVICON, this.getClass()).v2());
                    } catch (URISyntaxException | IOException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(NotFoundException::new);

        final HashCode hashCode = Hashing.sha256().hashBytes(fileContents);
        final EntityTag entityTag = new EntityTag(hashCode.toString());

        final Response.ResponseBuilder response = request.evaluatePreconditions(entityTag);
        if (response != null) {
            return response.build();
        }

        final String contentType = firstNonNull(mimeTypes.getContentType(FAVICON), MediaType.APPLICATION_OCTET_STREAM);

        return Response
                .ok(fileContents, contentType)
                .tag(entityTag)
                .build();
    }

    @Path(ASSETS_PREFIX + "/plugin/{plugin}/{filename}")
    @GET
    public Response get(@Context Request request,
                        @Context HttpHeaders headers,
                        @PathParam("plugin") String pluginName,
                        @PathParam("filename") String filename) {
        final Plugin plugin = getPluginForName(pluginName)
                .orElseThrow(() -> new NotFoundException("Couldn't find plugin " + pluginName));
        final var filenameWithoutSuffix = trimBasePath(filename, headers);

        try {
            final var resource = readFile(true, filenameWithoutSuffix, plugin.metadata().getClass());
            return getResponse(request, filenameWithoutSuffix, resource);
        } catch (URISyntaxException | IOException e) {
            throw new NotFoundException("Couldn't find " + filenameWithoutSuffix + " in plugin " + pluginName, e);
        }
    }

    private Optional<Plugin> getPluginForName(String pluginName) {
        return this.plugins.stream().filter(plugin -> plugin.metadata().getUniqueId().equals(pluginName)).findFirst();
    }

    @Path(ASSETS_PREFIX + "/{filename: .*}")
    @GET
    public Response get(@Context ContainerRequest request,
                        @Context HttpHeaders headers,
                        @PathParam("filename") String filename) {
        final var filenameWithoutSuffix = trimBasePath(filename, headers);
        try {
            final var resource = readFile(false, filenameWithoutSuffix, this.getClass());
            return getResponse(request, filenameWithoutSuffix, resource);
        } catch (IOException | URISyntaxException e) {
            return generateIndexHtml(headers, (String) request.getProperty(CSPDynamicFeature.CSP_NONCE_PROPERTY));
        }

    }

    private String trimBasePath(String filename, HttpHeaders headers) {
        final String baseUriPath = removeTrailingSlash(RestTools.buildRelativeExternalUri(headers.getRequestHeaders(), httpConfiguration.getHttpExternalUri()).getPath());
        return filename.startsWith(baseUriPath) ? filename.substring(baseUriPath.length()) : filename;
    }

    private String removeTrailingSlash(String basePath) {
        if (basePath == null || !basePath.endsWith("/")) {
            return basePath;
        }

        return basePath.substring(0, basePath.length() - 1);
    }

    @GET
    @Path("{filename:.*}")
    public Response getIndex(@Context ContainerRequest request,
                             @Context HttpHeaders headers) {
        final URI originalLocation = request.getRequestUri();
        return get(request, headers, originalLocation.getPath());
    }

    private Response getResponse(Request request, String filename, Tuple2<java.nio.file.Path, byte[]> resource) throws IOException, URISyntaxException {
        final byte[] fileContents = resource.v2();

        final FileTime lastModifiedTime = Files.getLastModifiedTime(resource.v1());
        final Date lastModified = Date.from(lastModifiedTime.toInstant());
        final HashCode hashCode = Hashing.sha256().hashBytes(fileContents);
        final EntityTag entityTag = new EntityTag(hashCode.toString());

        final Response.ResponseBuilder response = request.evaluatePreconditions(lastModified, entityTag);
        if (response != null) {
            return response.build();
        }

        final String contentType = firstNonNull(mimeTypes.getContentType(filename), MediaType.APPLICATION_OCTET_STREAM);
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) TimeUnit.DAYS.toSeconds(365));
        cacheControl.setNoCache(false);
        cacheControl.setPrivate(false);

        return Response
                .ok(fileContents, contentType)
                .tag(entityTag)
                .cacheControl(cacheControl)
                .lastModified(lastModified)
                .build();
    }

    private Tuple2<java.nio.file.Path, byte[]> readFile(boolean fromPlugin, String filename, Class<?> aClass) throws URISyntaxException, IOException {
        final URL resourceUrl = aClass.getResource(pluginPrefixFilename(fromPlugin, filename));
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource file " + filename + " not found.");
        }
        final URI uri = resourceUrl.toURI();

        switch (resourceUrl.getProtocol()) {
            case "file": {
                final var path = Paths.get(uri);
                return new Tuple2<>(path, Files.readAllBytes(path));
            }
            case "jar": {
                final FileSystem fileSystem = fileSystemCache.getUnchecked(uri);
                final java.nio.file.Path path = fileSystem.getPath(pluginPrefixFilename(fromPlugin, filename));
                return new Tuple2<>(path, Resources.toByteArray(resourceUrl));
            }
            default:
                throw new IllegalArgumentException("Not a JAR or local file: " + resourceUrl);
        }
    }

    @Nonnull
    private String pluginPrefixFilename(boolean fromPlugin, String filename) {
        if (fromPlugin) {
            return "/" + filename;
        } else {
            return "/" + PluginAssets.pathPrefix + "/" + filename;
        }
    }

    private Response generateIndexHtml(HttpHeaders headers, String nonce) {
        return Response
                .ok(indexHtmlGenerator.get(headers.getRequestHeaders(), nonce))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .header("X-UA-Compatible", "IE=edge")
                .build();
    }
}
