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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20220623125450_AddJobTypeToJobTriggerTest {

    private final V20220623125450_AddJobTypeToJobTrigger migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20220623125450_AddJobTypeToJobTriggerTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection(DBJobTriggerService.COLLECTION_NAME);
        migration = new V20220623125450_AddJobTypeToJobTrigger(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @BeforeEach
    public void setUp() {
        when(clusterConfigService.getOrDefault(any(), any())).thenReturn(V20220623125450_AddJobTypeToJobTrigger.MigrationStatus.createEmpty());
    }

    @Test
    @MongoDBFixtures("V20220623125450_AddJobTypeToJobTriggerTest.json")
    void upgrade() {
        long before = collection.countDocuments(Filters.exists(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, false));
        Assertions.assertThat(before).isEqualTo(3);

        migration.upgrade();

        long after = collection.countDocuments(Filters.exists(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, true));
        Assertions.assertThat(after).isEqualTo(3);

        final long eventTypeCount = collection.countDocuments(Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, "event-processor-execution-v1"));
        Assertions.assertThat(eventTypeCount).isEqualTo(2);
        final long notificationTypeCount = collection.countDocuments(Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, "notification-execution-v1"));
        Assertions.assertThat(notificationTypeCount).isEqualTo(1);
    }
}
