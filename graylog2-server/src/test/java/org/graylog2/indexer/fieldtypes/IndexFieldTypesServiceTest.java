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
package org.graylog2.indexer.fieldtypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class IndexFieldTypesServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private IndexFieldTypesService dbService;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        this.dbService = new IndexFieldTypesService(mongoRule.getMongoConnection(), objectMapperProvider);
    }

    @After
    public void tearDown() {
        mongoRule.getMongoConnection().getMongoDatabase().drop();
    }

    private IndexFieldTypes createDto(String indexName, Map<String, FieldType> fields) {
        return IndexFieldTypes.builder()
                .indexName(indexName)
                .fields(ImmutableMap.<String, FieldType>builder()
                        .put("message", FieldType.create("message", "text"))
                        .put("source", FieldType.create("source", "text"))
                        .put("timestamp", FieldType.create("timestamp", "date"))
                        .put("http_method", FieldType.create("http_method", "keyword"))
                        .put("http_status", FieldType.create("http_status", "long"))
                        .putAll(fields)
                        .build())
                .build();
    }

    @Test
    public void saveGetDeleteStream() {
        final IndexFieldTypes newDto1 = createDto("graylog_0", Collections.emptyMap());
        final IndexFieldTypes newDto2 = createDto("graylog_1", Collections.emptyMap());

        final IndexFieldTypes savedDto1 = dbService.save(newDto1);
        final IndexFieldTypes savedDto2 = dbService.save(newDto2);

        final IndexFieldTypes dto1 = dbService.get(savedDto1.id()).orElse(null);
        assertThat(dto1).as("check that saving the DTO worked").isNotNull();
        assertThat(dto1.id()).isNotBlank();
        assertThat(dto1.indexName()).isEqualTo("graylog_0");
        assertThat(dto1.fields()).containsOnly(
                Maps.immutableEntry("message", FieldType.create("message", "text")),
                Maps.immutableEntry("source", FieldType.create("source", "text")),
                Maps.immutableEntry("timestamp", FieldType.create("timestamp", "date")),
                Maps.immutableEntry("http_method", FieldType.create("http_method", "keyword")),
                Maps.immutableEntry("http_status", FieldType.create("http_status", "long"))
        );

        final IndexFieldTypes dto2 = dbService.get(savedDto2.indexName()).orElse(null);
        assertThat(dto2)
                .as("check that get by index_name works")
                .isNotNull()
                .extracting("indexName")
                .containsOnly("graylog_1");

        assertThat(dbService.streamAll().count())
                .as("check that all entries are returned as a stream")
                .isEqualTo(2);

        dbService.delete(dto1.id());
        assertThat(dbService.get(dto1.id())).as("check that delete works").isNotPresent();
    }
}