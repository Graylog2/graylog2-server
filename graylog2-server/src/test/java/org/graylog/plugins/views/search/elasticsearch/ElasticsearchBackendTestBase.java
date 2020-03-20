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
package org.graylog.plugins.views.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

abstract class ElasticsearchBackendTestBase {
    static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
    static final ObjectMapper objectMapper = objectMapperProvider.get();

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
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    List<String> indicesOf(MultiSearch clientRequest) throws IOException {
        final String request = clientRequest.getData(objectMapper);
        final String[] lines = request.split("\\r?\\n");
        final int noOfHeaders = lines.length / 2;
        return IntStream.range(0, noOfHeaders)
                .mapToObj(headerNumber -> {
                    try {
                        final JsonNode headerNode = objectMapper.readTree(lines[headerNumber * 2]);
                        return headerNode.get("index").asText();
                    } catch (IOException ignored) {}
                    return null;
                })
                .collect(Collectors.toList());
    }
}
