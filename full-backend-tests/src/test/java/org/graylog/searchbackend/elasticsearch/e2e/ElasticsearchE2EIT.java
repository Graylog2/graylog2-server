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
package org.graylog.searchbackend.elasticsearch.e2e;

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.graylognode.NodeContainerConfig.GELF_HTTP_PORT;

@GraylogBackendConfiguration
public class ElasticsearchE2EIT {
    private static GraylogApis api;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;
    }

    @FullBackendTest
    void inputMessageCanBeSearched() {
        int mappedPort = api.backend().mappedPortFor(GELF_HTTP_PORT);

        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, api.requestSpecificationSupplier());

        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"kram\", \"host\":\"example.org\", \"facility\":\"test\"}",
                api.requestSpecificationSupplier());

        List<String> messages = SearchUtils.searchForAllMessages(api.requestSpecificationSupplier());
        assertThat(messages).doesNotContain("Hello there");

        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"Hello there\", \"host\":\"example.org\", \"facility\":\"test\"}",
                api.requestSpecificationSupplier());

        assertThat(SearchUtils.waitForMessage(api.requestSpecificationSupplier(), "Hello there")).isTrue();
    }
}
