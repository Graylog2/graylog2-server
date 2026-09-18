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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.google.common.io.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.EntityTag;
import org.graylog2.web.PluginAssets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.CodeSource;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class ResourceFileReader {
    /**
     * Plugin assets are packaged at the root of the plugin JAR.
     */
    public static final String PLUGIN_ASSETS_DIR = "/";
    public static final String WEB_INTERFACE_ASSETS_DIR = "/" + PluginAssets.pathPrefix + "/";

    private final LoadingCache<URI, FileSystem> fileSystemCache;

    @Inject
    public ResourceFileReader() {
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

    public record ResourceFile(java.nio.file.Path path,
                               Supplier<byte[]> contents,
                               Supplier<EntityTag> entityTag) {
        public static ResourceFile create(java.nio.file.Path path, byte[] fileContents) {
            return new ResourceFile(path, () -> fileContents, Suppliers.memoize(() -> {
                final var hashCode = Hashing.sha256().hashBytes(fileContents);
                return new EntityTag(hashCode.toString());
            }));
        }

        public Optional<Date> lastModified() {
            return Optional.ofNullable(path())
                    .flatMap(path -> {
                        try {
                            final FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                            return Optional.of(Date.from(lastModifiedTime.toInstant()));
                        } catch (IOException e) {
                            return Optional.empty();
                        }
                    });
        }
    }

    public ResourceFile readFile(String filename, Class<?> aClass) throws URISyntaxException, IOException {
        return readFileFrom(WEB_INTERFACE_ASSETS_DIR, filename, aClass);
    }

    /**
     * Reads an asset of the plugin that {@code pluginClass} belongs to.
     * <p>
     * Plugin assets are packaged at the root of the plugin JAR, which is also where every other JAR on the
     * classpath keeps its own root-level resources. A classpath lookup delegates beyond the plugin, and a
     * traversal sequence normalizes away at the root ({@code "/../../log4j2.xml"} becomes
     * {@code "/log4j2.xml"}), so {@link #resolveResourceName(String, String)} alone cannot keep this from
     * addressing an unrelated JAR. We therefore additionally require the resource to originate from the
     * plugin's own code source.
     */
    public ResourceFile readFileFromPlugin(String filename, Class<?> pluginClass) throws URISyntaxException, IOException {
        final String resourceName = resolveResourceName(PLUGIN_ASSETS_DIR, filename);
        if (!isFromCodeSourceOf(pluginClass.getResource(resourceName), pluginClass)) {
            throw new FileNotFoundException("Resource file not found.");
        }
        return readFileFrom(PLUGIN_ASSETS_DIR, filename, pluginClass);
    }

    public ResourceFile readFileFrom(String resourceDir, String filename, Class<?> aClass) throws URISyntaxException, IOException {
        final String resourceName = resolveResourceName(resourceDir, filename);
        final URL resourceUrl = aClass.getResource(resourceName);
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource file not found.");
        }
        final URI uri = resourceUrl.toURI();

        switch (resourceUrl.getProtocol()) {
            case "file": {
                final var path = Paths.get(uri);
                final var contents = Files.readAllBytes(path);
                return ResourceFile.create(path, contents);
            }
            case "jar": {
                final FileSystem fileSystem = fileSystemCache.getUnchecked(uri);
                final java.nio.file.Path path = fileSystem.getPath(resourceName);
                final var contents = Resources.toByteArray(resourceUrl);
                return ResourceFile.create(path, contents);
            }
            default:
                throw new IllegalArgumentException("Not a JAR or local file: " + resourceUrl);
        }
    }

    /**
     * Resolves {@code filename} inside {@code resourceDir} and verifies that the result is a direct child of
     * it, so a path traversal sequence in the (already URL-decoded) filename cannot address resources outside
     * of the directory.
     * <p>
     * The check requires a direct child rather than mere containment below {@code resourceDir}, because all
     * asset directories we serve from are flat, and because containment is meaningless for
     * {@link #PLUGIN_ASSETS_DIR}: {@code "/"} contains every path, including a traversed one.
     *
     * @return the canonical classpath resource name of the requested file
     * @throws FileNotFoundException if the filename does not address a direct child of {@code resourceDir}
     */
    @VisibleForTesting
    @Nonnull
    static String resolveResourceName(String resourceDir, String filename) throws FileNotFoundException {
        final java.nio.file.Path base = Paths.get(resourceDir).normalize();
        final java.nio.file.Path resolved;
        try {
            resolved = base.resolve(filename).normalize();
        } catch (InvalidPathException e) {
            throw new FileNotFoundException("Invalid resource file name.");
        }
        if (!base.equals(resolved.getParent())) {
            throw new FileNotFoundException("Invalid resource file name.");
        }
        // Rebuild the name from the validated single path segment: classpath resource names always use "/"
        // as a separator, whereas Path#toString() would use the platform-dependent one.
        return resourceDir.endsWith("/")
                ? resourceDir + resolved.getFileName()
                : resourceDir + "/" + resolved.getFileName();
    }

    /**
     * Checks whether {@code resourceUrl} was loaded from the same JAR or classes directory that
     * {@code aClass} itself came from.
     */
    @VisibleForTesting
    static boolean isFromCodeSourceOf(@Nullable URL resourceUrl, Class<?> aClass) {
        if (resourceUrl == null) {
            return false;
        }
        final CodeSource codeSource = aClass.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            // Without a code source there is no way to tell where the resource came from, so refuse to
            // serve it rather than falling back to the whole classpath.
            return false;
        }

        // Compare local paths rather than URL strings, so that the two sides cannot differ merely in how they
        // percent-encode a location, for example an installation directory containing a space.
        final var codeSourcePath = toLocalPath(codeSource.getLocation().toString());
        final var resourcePath = toLocalPath("jar".equals(resourceUrl.getProtocol())
                ? jarLocation(resourceUrl)
                : resourceUrl.toString());
        if (codeSourcePath.isEmpty() || resourcePath.isEmpty()) {
            return false;
        }

        // A resource in a JAR resolves to the JAR itself, one on an exploded classpath to a file below the
        // classes directory. Path#startsWith matches whole path segments, so a sibling directory with a
        // common name prefix does not pass.
        return resourcePath.get().startsWith(codeSourcePath.get());
    }

    private static Optional<java.nio.file.Path> toLocalPath(String url) {
        try {
            return Optional.of(Paths.get(URI.create(url)));
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Strips the entry from a {@code jar:} URL, turning
     * {@code jar:file:/opt/graylog/plugin/example.jar!/abc.js} into
     * {@code file:/opt/graylog/plugin/example.jar}.
     */
    private static String jarLocation(URL resourceUrl) {
        final String url = resourceUrl.toString();
        final int entrySeparator = url.indexOf("!/");
        return entrySeparator < 0 ? url : url.substring("jar:".length(), entrySeparator);
    }
}
