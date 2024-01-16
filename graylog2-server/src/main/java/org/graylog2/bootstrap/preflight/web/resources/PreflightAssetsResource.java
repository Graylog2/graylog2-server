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
package org.graylog2.bootstrap.preflight.web.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import org.graylog2.bootstrap.preflight.PreflightConstants;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nonnull;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

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
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

@Path("/")
public class PreflightAssetsResource {
    private final MimetypesFileTypeMap mimeTypes;
    private final LoadingCache<URI, FileSystem> fileSystemCache;

    @Inject
    public PreflightAssetsResource(MimetypesFileTypeMap mimeTypes) {
        this.mimeTypes = mimeTypes;
        this.fileSystemCache = CacheBuilder.newBuilder()
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

    @Produces(MediaType.TEXT_HTML)
    @GET
    public Response index(@Context Request request) {
        return this.get(request, "index.html");
    }

    @Path("/{filename}")
    @GET
    public Response get(@Context Request request, @PathParam("filename") String filename) {
        try {
            final URL resourceUrl = getResourceUri(filename);
            return getResponse(request, filename, resourceUrl);
        } catch (IOException | URISyntaxException e) {
            throw new NotFoundException("Couldn't find " + filename, e);
        }
    }

    private URL getResourceUri(String filename) throws FileNotFoundException {
        final URL resourceUrl = this.getClass().getResource(PreflightConstants.ASSETS_RESOURCE_DIR + filename);
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource file " + filename + " not found.");
        }
        return resourceUrl;
    }

    private Response getResponse(Request request, String filename, URL resourceUrl) throws IOException, URISyntaxException {
        final URI uri = resourceUrl.toURI();

        final java.nio.file.Path path;
        final byte[] fileContents;
        switch (resourceUrl.getProtocol()) {
            case "file" -> {
                path = Paths.get(uri);
                fileContents = Files.readAllBytes(path);
            }
            case "jar" -> {
                final FileSystem fileSystem = fileSystemCache.getUnchecked(uri);
                path = fileSystem.getPath(PreflightConstants.ASSETS_RESOURCE_DIR + filename);
                fileContents = Resources.toByteArray(resourceUrl);
            }
            default -> throw new IllegalArgumentException("Not a JAR or local file: " + resourceUrl);
        }

        final FileTime lastModifiedTime = Files.getLastModifiedTime(path);
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
}
