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

import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.util.LinkedHashSet;

public class MockedOpensearchClient {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final LinkedHashSet<MockedResponse> responses = new LinkedHashSet<>();

        public Builder response(String method, String url, String body) {
            responses.add(new MockedResponse(method, url, body));
            return this;
        }

        public  OfficialOpensearchClient build() {
            final MockedTransport transport = new MockedTransport(responses);
            return new OfficialOpensearchClient(new OpenSearchClient(transport), new OpenSearchAsyncClient(transport));
        }
    }
}
