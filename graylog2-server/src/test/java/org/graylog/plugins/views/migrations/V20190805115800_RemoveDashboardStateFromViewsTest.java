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
package org.graylog.plugins.views.migrations;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class V20190805115800_RemoveDashboardStateFromViewsTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private ClusterConfigService clusterConfigService;

    @Test
    @MongoDBFixtures("V20190805115800_RemoveDashboardStateFromViewsTest.json")
    public void removesDashboardStateFromExistingViews() {
        final Migration migration = new V20190805115800_RemoveDashboardStateFromViews(clusterConfigService, mongodb.mongoConnection());

        migration.upgrade();

        final ArgumentCaptor<V20190805115800_RemoveDashboardStateFromViews.MigrationCompleted> argumentCaptor = ArgumentCaptor.forClass(V20190805115800_RemoveDashboardStateFromViews.MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().modifiedViewsCount()).isEqualTo(4);

        MongoCollection<Document> collection = mongodb.mongoConnection().getMongoDatabase().getCollection("views");
        assertThat(collection.count()).isEqualTo(4);
    }
}
