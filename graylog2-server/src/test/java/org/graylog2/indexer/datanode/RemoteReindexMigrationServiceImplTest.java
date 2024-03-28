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
package org.graylog2.indexer.datanode;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.OptionalAssert;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.indexer.migration.LogEntry;
import org.graylog2.indexer.migration.LogLevel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class RemoteReindexMigrationServiceImplTest {

    private RemoteReindexMigrationService migrationService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider) {
        migrationService = new RemoteReindexMigrationServiceImpl(mongodb.mongoConnection(), mongoJackObjectMapperProvider);
    }

    @Test
    void testCreateReadUpdate() {
        MigrationConfiguration migration = migrationService.saveMigration(MigrationConfiguration.forIndices(List.of("index_1", "index_2", "errors_1")));
        final String migrationID = migration.id();
        Assertions.assertThat(migration)
                .satisfies(m -> Assertions.assertThat(m.id()).isNotEmpty())
                .satisfies(m -> Assertions.assertThat(m.indices()).hasSize(3));


        migrationService.assignTask(migrationID, "index_1", "task_1");
        migrationService.assignTask(migrationID, "index_2", "task_2");
        migrationService.assignTask(migrationID, "errors_1", "task_3");


        Assertions.assertThat(migrationService.getMigration(migrationID))
                .isPresent()
                .hasValueSatisfying(m -> {
                    assertIndexAndTaskMapping(m, "index_1", "task_1");
                    assertIndexAndTaskMapping(m, "index_2", "task_2");
                    assertIndexAndTaskMapping(m, "errors_1", "task_3");
                });
    }

    private static OptionalAssert<IndexMigrationConfiguration> assertIndexAndTaskMapping(MigrationConfiguration m, String indexName, String taskName) {
        return Assertions.assertThat(m.getConfigForIndexName(indexName))
                .isPresent()
                .hasValueSatisfying(i -> Assertions.assertThat(i.taskId()).hasValue(taskName));
    }

    @Test
    void testLogs() {
        MigrationConfiguration migration = migrationService.saveMigration(MigrationConfiguration.forIndices(List.of("index_1", "index_2", "errors_1")));
        migrationService.appendLogEntry(migration.id(), new LogEntry(DateTime.now(DateTimeZone.UTC), LogLevel.INFO, "info entry"));
        migrationService.appendLogEntry(migration.id(), new LogEntry(DateTime.now(DateTimeZone.UTC), LogLevel.ERROR, "error entry"));
        Assertions.assertThat(migrationService.getMigration(migration.id()))
                .hasValueSatisfying(m -> {
                    Assertions.assertThat(m.logs())
                            .hasSize(2)
                            .extracting(LogEntry::message)
                            .contains("info entry", "error entry");
                });
    }
}
