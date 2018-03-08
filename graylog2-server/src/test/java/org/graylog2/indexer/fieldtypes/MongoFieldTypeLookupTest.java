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
import com.google.common.collect.ImmutableSet;
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
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class MongoFieldTypeLookupTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private IndexFieldTypesService dbService;
    private MongoFieldTypeLookup lookup;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        this.dbService = new IndexFieldTypesService(mongoRule.getMongoConnection(), objectMapperProvider);
        this.lookup = new MongoFieldTypeLookup(dbService, new FieldTypeMapper());
    }

    @After
    public void tearDown() {
        mongoRule.getMongoConnection().getMongoDatabase().drop();
    }

    private IndexFieldTypesDTO createDto(String indexName, String indexSetId, Set<FieldTypeDTO> fields) {
        return IndexFieldTypesDTO.builder()
                .indexName(indexName)
                .indexSetId(indexSetId)
                .fields(ImmutableSet.<FieldTypeDTO>builder()
                        .add(FieldTypeDTO.create("message", "text"))
                        .add(FieldTypeDTO.create("source", "text"))
                        .add(FieldTypeDTO.create("timestamp", "date"))
                        .add(FieldTypeDTO.create("http_method", "keyword"))
                        .add(FieldTypeDTO.create("http_status", "long"))
                        .addAll(fields)
                        .build())
                .build();
    }

    @Test
    public void getSingleField() {
        dbService.save(createDto("graylog_0", "abc", Collections.emptySet()));
        dbService.save(createDto("graylog_1", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_2", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_3", "xyz", of(
                FieldTypeDTO.create("yolo1", "text")
        )));

        final FieldTypes result = lookup.get("message").orElse(null);

        assertThat(result).isNotNull();
        assertThat(result.fieldName()).isEqualTo("message");
        assertThat(result.types()).containsOnly(FieldTypes.Type.builder()
                .type("string")
                .properties(of("full-text-search"))
                .indexNames(of("graylog_0", "graylog_1", "graylog_2", "graylog_3"))
                .build());
    }

    @Test
    public void getMultipleFields() {
        dbService.save(createDto("graylog_0", "abc", Collections.emptySet()));
        dbService.save(createDto("graylog_1", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_2", "xyz", of(
                FieldTypeDTO.create("yolo1", "boolean")
        )));
        dbService.save(createDto("graylog_3", "xyz", of(
                FieldTypeDTO.create("yolo1", "text")
        )));

        final Map<String, FieldTypes> result = lookup.get(of("yolo1", "timestamp"));

        assertThat(result).containsOnlyKeys("yolo1", "timestamp");

        assertThat(result.get("yolo1").fieldName()).isEqualTo("yolo1");
        assertThat(result.get("yolo1").types()).hasSize(2);
        assertThat(result.get("yolo1").types()).containsOnly(
                FieldTypes.Type.builder()
                        .type("string")
                        .properties(of("full-text-search"))
                        .indexNames(of("graylog_3"))
                        .build(),
                FieldTypes.Type.builder()
                        .type("boolean")
                        .properties(of("enumerable"))
                        .indexNames(of("graylog_2"))
                        .build()
        );

        assertThat(result.get("timestamp").fieldName()).isEqualTo("timestamp");
        assertThat(result.get("timestamp").types()).hasSize(1);
        assertThat(result.get("timestamp").types()).containsOnly(FieldTypes.Type.builder()
                .type("date")
                .properties(of("enumerable"))
                .indexNames(of("graylog_0", "graylog_1", "graylog_2", "graylog_3"))
                .build());
    }

    @Test
    public void getMultipleFieldsWithIndexScope() {
        dbService.save(createDto("graylog_0", "abc", Collections.emptySet()));
        dbService.save(createDto("graylog_1", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_2", "xyz", of(
                FieldTypeDTO.create("yolo1", "boolean")
        )));
        dbService.save(createDto("graylog_3", "xyz", of(
                FieldTypeDTO.create("yolo1", "text")
        )));

        final Map<String, FieldTypes> result = lookup.get(of("yolo1", "timestamp"), of("graylog_1", "graylog_2"));

        assertThat(result).containsOnlyKeys("yolo1", "timestamp");

        assertThat(result.get("yolo1").fieldName()).isEqualTo("yolo1");
        assertThat(result.get("yolo1").types()).hasSize(1);
        assertThat(result.get("yolo1").types()).containsOnly(
                FieldTypes.Type.builder()
                        .type("boolean")
                        .properties(of("enumerable"))
                        .indexNames(of("graylog_2"))
                        .build()
        );

        assertThat(result.get("timestamp").fieldName()).isEqualTo("timestamp");
        assertThat(result.get("timestamp").types()).hasSize(1);
        assertThat(result.get("timestamp").types()).containsOnly(FieldTypes.Type.builder()
                .type("date")
                .properties(of("enumerable"))
                .indexNames(of("graylog_1", "graylog_2"))
                .build());
    }
}