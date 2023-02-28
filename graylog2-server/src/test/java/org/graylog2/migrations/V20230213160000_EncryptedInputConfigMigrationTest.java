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
package org.graylog2.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.inputs.encryption.EncryptedInputConfigMigration;
import org.graylog2.inputs.encryption.EncryptedInputConfigMigrationTest;
import org.graylog2.inputs.encryption.EncryptedInputConfigs;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.migrations.V20230213160000_EncryptedInputConfigMigration.MigrationCompleted;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MongoDBFixtures("V20230213160000_EncryptedInputConfigMigrationTest.json")
public class V20230213160000_EncryptedInputConfigMigrationTest extends EncryptedInputConfigMigrationTest {


    @Override
    @BeforeEach
    public void setUp(MongoDBTestService mongodb) throws Exception {
        when(clusterConfigService.getOrDefault(MigrationCompleted.class, new MigrationCompleted(Map.of())))
                .thenReturn(new MigrationCompleted(Map.of()));

        collection = mongodb.mongoConnection().getMongoDatabase().getCollection("inputs");
        super.setUp(mongodb);
    }

    @Override
    protected EncryptedInputConfigMigration createMigration(MongoDBTestService mongodb,
                                                            MessageInputFactory messageInputFactory,
                                                            ObjectMapper objectMapper) {
        return new V20230213160000_EncryptedInputConfigMigration(clusterConfigService,
                mongodb.mongoConnection(), messageInputFactory, objectMapper);
    }

    @Test
    public void migrationCompleted() {
        migration.upgrade();

        final V20230213160000_EncryptedInputConfigMigration.MigrationCompleted migrationCompleted = new V20230213160000_EncryptedInputConfigMigration.MigrationCompleted(Map.of(
                JsonPathInput.class.getCanonicalName(),
                EncryptedInputConfigs.getEncryptedFields(fakeJsonPathInputConfig())
        ));

        verify(clusterConfigService, times(1)).write(migrationCompleted);

        // pretend that all fields have been migrated already
        when(clusterConfigService.getOrDefault(V20230213160000_EncryptedInputConfigMigration.MigrationCompleted.class, new V20230213160000_EncryptedInputConfigMigration.MigrationCompleted(Map.of())))
                .thenReturn(migrationCompleted);

        migration.upgrade();

        // this time, we should have aborted early and not persist the completion marker again
        verify(clusterConfigService, times(1)).write(migrationCompleted);
    }

}
