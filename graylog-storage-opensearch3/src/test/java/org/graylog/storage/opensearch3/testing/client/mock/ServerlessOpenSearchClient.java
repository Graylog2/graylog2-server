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
package org.graylog.storage.opensearch3.testing.client.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.LinkedHashSet;

/**
 * This is a builder for stubbed/mocked opensearch client. The magic happens
 * in {@link StringResponse}, which will serve you your pre-registered json data, initially
 * provided by {@link ServerlessOpenSearchClient.Builder#stubResponse(String, String, String)} calls.
 *
 * Main usage should be during conversion of existing heavily mocked tests where
 * we have predefined json responses with expected data and structures.
 */
public class ServerlessOpenSearchClient {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LinkedHashSet<MockedResponse> responses = new LinkedHashSet<>();

        public Builder stubResponse(String method, String url, String body) {
            responses.add(new StringResponse(method, compilePattern(url), body));
            return this;
        }

        private PathMatcher compilePattern(String glob) {
            // todo: this is not good, but works as expected for now
            return FileSystems.getDefault().getPathMatcher("glob:" + glob);
        }

        public OfficialOpensearchClient build() {
            final MockedTransport transport = new MockedTransport(responses);
            return new OfficialOpensearchClient(new OpenSearchClient(transport), new OpenSearchAsyncClient(transport), new ObjectMapper());
        }

        public Builder stubResponse(String method, String url, URL resource) {
            responses.add(new ResourceResponse(method, compilePattern(url), resource));
            return this;
        }

        public Builder stubError(String method, String url, int httpErrorCode, String error) {
            responses.add(new ErrorResponse(method, compilePattern(url), httpErrorCode, error));
            return this;
        }
    }
}
