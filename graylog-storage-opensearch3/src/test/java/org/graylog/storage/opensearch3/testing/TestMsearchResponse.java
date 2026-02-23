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
package org.graylog.storage.opensearch3.testing;

import com.google.common.io.Resources;
import jakarta.json.stream.JsonParser;
import org.apache.commons.io.FileUtils;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.jackson.JacksonJsonProvider;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.core.MsearchResponse;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestMsearchResponse {
    public static MsearchResponse<JsonData> fromFixture(String filename) throws IOException {
        return resultFor(resourceFile(filename));
    }

    private static MsearchResponse<JsonData> resultFor(String result) throws IOException {
        OSSerializationUtils utils = new OSSerializationUtils();
        JsonpDeserializer<MsearchResponse<JsonData>> deserializer = MsearchResponse.createMsearchResponseDeserializer(JsonData._DESERIALIZER);
        try (StringReader reader = new StringReader(result)) {
            JsonParser parser = JacksonJsonProvider.provider().createParser(reader);
            JacksonJsonpMapper mapper = new JacksonJsonpMapper();
            return deserializer.deserialize(parser, mapper);
        }
    }

    private static String resourceFile(String filename) {
        try {
            final URL resource = Resources.getResource(filename);
            final Path path = Paths.get(resource.toURI());
            return FileUtils.readFileToString(path.toFile(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
