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

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class V20190127111728_MigrateWidgetFormatSettingsTest {
    @Rule
    public final MongoDBInstance mongoDB = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20190127111728_MigrateWidgetFormatSettings migration;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() {
        migration = new V20190127111728_MigrateWidgetFormatSettings(mongoDB.mongoConnection(), clusterConfigService);
    }

    @Test
    @MongoDBFixtures("V20190127111728_MigrateWidgetFormatSettings.json")
    public void testMigration() {
        final BasicDBObject dbQuery1 = new BasicDBObject();
        dbQuery1.put("_id", new ObjectId("5e2ee372b22d7970576b2eb3"));
        final MongoCollection<Document> collection = mongoDB.mongoConnection()
                .getMongoDatabase()
                .getCollection("views");
        migration.upgrade();
        final FindIterable<Document> views = collection.find(dbQuery1);
        final Document view1 = views.first();

        @SuppressWarnings("unchecked")
        final List<Document> widgets1 = (List) view1.get("state", Document.class).get("2c67cc0f-c62e-47c1-8b70-e3198925e6bc", Document.class).get("widgets");
        assertThat(widgets1.size()).isEqualTo(2);
        Set<Document> aggregationWidgets =widgets1.stream().filter(w -> w.getString("type")
                .equals("aggregation")).collect(Collectors.toSet());
        assertThat(aggregationWidgets.size()).isEqualTo(1);
        final Document aggregationWidget = aggregationWidgets.iterator().next();
        final Document config = aggregationWidget.get("config", Document.class);
        final Document formattingSettings = config.get("formatting_settings", Document.class);

        @SuppressWarnings("unchecked")
        final List<Document> chartColors = (List) formattingSettings.get("chart_colors", List.class);
        assertThat(chartColors.size()).isEqualTo(1);
        final Document chartColor = chartColors.get(0);
        assertThat(chartColor.getString("field_name")).isEqualTo("count()");
        assertThat(chartColor.getString("chart_color")).isEqualTo("#e91e63");
    }
}
