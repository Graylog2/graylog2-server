/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
