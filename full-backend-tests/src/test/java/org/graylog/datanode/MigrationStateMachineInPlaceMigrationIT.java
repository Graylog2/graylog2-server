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
package org.graylog.datanode;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.graylog.shaded.kafka09.joptsimple.internal.Strings;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateIndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateIndexResponse;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog.storage.opensearch2.testing.OpenSearchInstanceBuilder;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.DATANODE_DEV)
public class MigrationStateMachineInPlaceMigrationIT implements MigrationITTools {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationStateMachineInPlaceMigrationIT.class);

    private GraylogApis apis;
    private OpenSearchInstance openSearchInstance;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
        openSearchInstance = OpenSearchInstanceBuilder.builder()
                .network(apis.backend().network())
                .hostname("existing-opensearch-cluster")
                .build();
    }

    @AfterEach
    void tearDown() {
        this.openSearchInstance.close();
    }

    @ContainerMatrixTest
    void testMigration() {
        ValidatableResponse response = apis.get("/migration/state", 200);
        verify(apis.get("/migration/state", 200), MigrationState.NEW, MigrationStep.SELECT_MIGRATION);
        verify(apis.post("/migration/trigger", request(MigrationStep.SELECT_MIGRATION), 200), MigrationState.MIGRATION_WELCOME_PAGE, MigrationStep.SHOW_DIRECTORY_COMPATIBILITY_CHECK);
    }
}
