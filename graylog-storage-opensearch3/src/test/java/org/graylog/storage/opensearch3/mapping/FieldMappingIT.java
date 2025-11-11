package org.graylog.storage.opensearch3.mapping;

import org.assertj.core.api.Assertions;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.opensearch.client.util.ObjectBuilder;

import java.util.Map;
import java.util.function.Function;

public class FieldMappingIT {

    public static final String INDEX_NAME = "graylog_42";
    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();


    @BeforeEach
    void setUp() {

        openSearchInstance.getOfficialOpensearchClient().sync(c -> c.indices().create(req -> req.index(INDEX_NAME)), "Failed to create index " + INDEX_NAME);

        final Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>> graylog42 = r -> r.index(INDEX_NAME)
                .properties("action", p -> p.keyword(keyword -> keyword))
                .properties("text", p -> p.text(text -> text.fielddata(true).analyzer("simple")))
                .properties("date", p -> p.date(date -> date))
                .properties("number", p -> p.long_(long_ -> long_));

        openSearchInstance.getOfficialOpensearchClient().sync(c -> c.indices().putMapping(graylog42), "Failed to put mapping to index " + INDEX_NAME);
    }

    @Test
    void testMapping() {
        final FieldMappingApi api = new FieldMappingApi(openSearchInstance.getOfficialOpensearchClient());

        final Map<String, FieldMappingApi.FieldMapping> fieldTypes = api.fieldTypes(INDEX_NAME);
        Assertions.assertThat(fieldTypes)
                        .hasSize(4)
                        .containsEntry("date", new FieldMappingApi.FieldMapping("date", false))
                        .containsEntry("number", new FieldMappingApi.FieldMapping("long", false))
                        .containsEntry("action", new FieldMappingApi.FieldMapping("keyword", false))
                        .containsEntry("text", new FieldMappingApi.FieldMapping("text", true));
    }
}
