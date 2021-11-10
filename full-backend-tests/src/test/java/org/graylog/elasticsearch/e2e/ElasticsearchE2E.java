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
package org.graylog.elasticsearch.e2e;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.SearchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.VM;
import static org.graylog.testing.containermatrix.ContainerVersions.ES6;
import static org.graylog.testing.containermatrix.ContainerVersions.ES7;
import static org.graylog.testing.containermatrix.ContainerVersions.MONGO3;
import static org.graylog.testing.containermatrix.ContainerVersions.MONGO4;
import static org.graylog.testing.graylognode.NodeContainerConfig.GELF_HTTP_PORT;

@ContainerMatrixTestsConfiguration(serverLifecycle = VM, esVersions = {ES6, ES7}, mongoVersions = {MONGO3, MONGO4})
public class ElasticsearchE2E {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchE2E.class);

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public ElasticsearchE2E(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @ContainerMatrixTest
    void inputMessageCanBeSearched() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);

        GelfInputUtils.createGelfHttpInput(mappedPort, GELF_HTTP_PORT, requestSpec);

        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"kram\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        List<String> messages = SearchUtils.searchForAllMessages(requestSpec);
        assertThat(messages).doesNotContain("Hello there");

        GelfInputUtils.postMessage(mappedPort,
                "{\"short_message\":\"Hello there\", \"host\":\"example.org\", \"facility\":\"test\"}",
                requestSpec);

        assertThat(SearchUtils.searchForMessage(requestSpec, "Hello there")).isTrue();
    }
}
