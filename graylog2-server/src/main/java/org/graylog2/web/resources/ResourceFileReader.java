package org.graylog2.web.resources;

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
import java.util.function.Supplier;

@Singleton
public class ResourceFileReader {
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
        return readFile(false, filename, aClass);
    }

    public ResourceFile readFileFromPlugin(String filename, Class<?> aClass) throws URISyntaxException, IOException {
        return readFile(true, filename, aClass);
    }

    private ResourceFile readFile(boolean fromPlugin, String filename, Class<?> aClass) throws URISyntaxException, IOException {
        final URL resourceUrl = aClass.getResource(pluginPrefixFilename(fromPlugin, filename));
        if (resourceUrl == null) {
            throw new FileNotFoundException("Resource file " + filename + " not found.");
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
                final java.nio.file.Path path = fileSystem.getPath(pluginPrefixFilename(fromPlugin, filename));
                final var contents = Resources.toByteArray(resourceUrl);
                return ResourceFile.create(path, contents);
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
}
