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
package org.graylog2.migrations;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.database.MongoConnectionRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static org.assertj.core.api.Assertions.assertThat;

public class V20180718155800_AddContentPackIdAndRevTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private V20180718155800_AddContentPackIdAndRev migration;

    @Before
    public void setUp() {
        this.migration = new V20180718155800_AddContentPackIdAndRev(mongoRule.getMongoConnection());
    }

    @Test
    public void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2018-07-18T15:58:00Z"));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void upgrade() {
        final MongoCollection<Document> collection = mongoRule.getMongoConnection()
                .getMongoDatabase()
                .getCollection(ContentPackPersistenceService.COLLECTION_NAME);
        final Bson filter = and(exists(ContentPack.FIELD_META_ID), exists(ContentPack.FIELD_META_REVISION));

        assertThat(collection.count(filter)).isEqualTo(1L);
        migration.upgrade();
        assertThat(collection.count(filter)).isEqualTo(2L);
    }
}