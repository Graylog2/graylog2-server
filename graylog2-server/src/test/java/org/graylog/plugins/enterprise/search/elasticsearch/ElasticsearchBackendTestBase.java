package org.graylog.plugins.enterprise.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import io.searchbox.core.MultiSearchResult;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

abstract class ElasticsearchBackendTestBase {
    static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    MultiSearchResult resultFor(String result) {
        final ObjectMapper objectMapper = objectMapperProvider.get();
        final MultiSearchResult multiSearchResult = new MultiSearchResult(objectMapper);
        multiSearchResult.setSucceeded(true);
        try {
            multiSearchResult.setJsonObject(objectMapper.readTree(result));
            return multiSearchResult;
        } catch (IOException e) {
        }
        return null;
    }

    String resourceFile(String filename) {
        try {
            final URL resource = this.getClass().getResource(filename);
            final Path path = Paths.get(resource.toURI());
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, Charsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
