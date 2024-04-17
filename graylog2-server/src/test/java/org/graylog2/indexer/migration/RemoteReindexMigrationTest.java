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
package org.graylog2.indexer.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

class RemoteReindexMigrationTest {

    @Test
    void testProgress() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.FINISHED),
                index("two", RemoteReindexingMigrationAdapter.Status.FINISHED),
                index("three", RemoteReindexingMigrationAdapter.Status.ERROR),
                index("four", RemoteReindexingMigrationAdapter.Status.RUNNING),
                index("five", RemoteReindexingMigrationAdapter.Status.NOT_STARTED)
        );
        Assertions.assertThat(migration.progress()).isEqualTo(60);
    }

    @Nonnull
    private static RemoteReindexIndex index(String indexName, RemoteReindexingMigrationAdapter.Status status) {

        final IndexMigrationProgress progress = switch (status) {
            case FINISHED -> new IndexMigrationProgress(100, 100, 0, 0);
            case ERROR -> new IndexMigrationProgress(100, 100, 0, 0);
            default -> new IndexMigrationProgress(100, 0, 0, 0);
        };
        return new RemoteReindexIndex(indexName, status, null, null, progress, null);
    }

    @Test
    void testProgressOneIndex() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.FINISHED)
        );
        Assertions.assertThat(migration.progress()).isEqualTo(100);
    }

    @Test
    void testProgressOneIndexNotStarted() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.NOT_STARTED)
        );
        Assertions.assertThat(migration.progress()).isEqualTo(0);
    }

    @Test
    void testProgressNoIndex() {
        final RemoteReindexMigration migration = withIndices();
        Assertions.assertThat(migration.progress()).isEqualTo(100);
    }

    @Test
    void testSerializeToJson() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapperProvider().get();
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.FINISHED),
                index("two", RemoteReindexingMigrationAdapter.Status.RUNNING)
        );
        final String serialized = mapper.writeValueAsString(migration);
        final Integer progress = JsonPath.parse(serialized).read("progress");
        Assertions.assertThat(progress).isEqualTo(50);
    }

    @Nonnull
    private static RemoteReindexMigration withIndices(RemoteReindexIndex... indices) {
        return new RemoteReindexMigration(UUID.randomUUID().toString(), Arrays.asList(indices), Collections.emptyList());
    }

    @Test
    void testStatusNotStarted() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.NOT_STARTED),
                index("two", RemoteReindexingMigrationAdapter.Status.NOT_STARTED)
        );
        Assertions.assertThat(migration.status()).isEqualTo(RemoteReindexingMigrationAdapter.Status.NOT_STARTED);
    }

    @Test
    void testStatusCompletedWithError() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.FINISHED),
                index("two", RemoteReindexingMigrationAdapter.Status.ERROR)
        );
        Assertions.assertThat(migration.status()).isEqualTo(RemoteReindexingMigrationAdapter.Status.ERROR);
    }


    @Test
    void testStatusRunningWithError() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.FINISHED),
                index("two", RemoteReindexingMigrationAdapter.Status.ERROR),
                index("three", RemoteReindexingMigrationAdapter.Status.RUNNING)
        );
        Assertions.assertThat(migration.status()).isEqualTo(RemoteReindexingMigrationAdapter.Status.RUNNING);
    }

    @Test
    void testStatusFinished() {
        final RemoteReindexMigration migration = withIndices(
                index("one", RemoteReindexingMigrationAdapter.Status.FINISHED),
                index("two", RemoteReindexingMigrationAdapter.Status.FINISHED)
        );
        Assertions.assertThat(migration.status()).isEqualTo(RemoteReindexingMigrationAdapter.Status.FINISHED);
    }
}
