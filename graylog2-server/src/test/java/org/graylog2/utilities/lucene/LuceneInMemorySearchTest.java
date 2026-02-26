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
package org.graylog2.utilities.lucene;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.assertj.core.api.Assertions;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQueryField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class LuceneInMemorySearchTest {

    private LuceneInMemorySearchEngine<SearchableItem> search;

    @BeforeEach
    void setUp() {
        final List<EntityAttribute> attributes = List.of(
                EntityAttribute.builder().id("name").title("Name").type(SearchQueryField.Type.STRING).sortable(true).searchable(true).build(),
                EntityAttribute.builder().id("age").title("Age").type(SearchQueryField.Type.INT).sortable(true).searchable(true).build()
        );
        search = new LuceneInMemorySearchEngine<>("name", attributes, () -> List.of(
                new SearchableItem("John Doe", 25),
                new SearchableItem("Max Mustermann", 30),
                new SearchableItem("John Smith", 35),
                new SearchableItem("Jane Doe", 40),
                new SearchableItem("Alan Smithee", 45)
        ));
    }

    @Test
    void testSearching() throws QueryNodeException, IOException {
        Assertions.assertThat(search.search("name:john", "name", SortOrder.ASCENDING, 1, 10))
                .hasSize(2)
                .extracting(SearchableItem::name)
                .contains("John Doe", "John Smith");

        Assertions.assertThat(search.search("name:max", "name", SortOrder.ASCENDING, 1, 10))
                .hasSize(1)
                .extracting(SearchableItem::name)
                .contains("Max Mustermann");

        Assertions.assertThat(search.search("name:john AND name:doe", "name", SortOrder.ASCENDING, 1, 10))
                .hasSize(1)
                .extracting(SearchableItem::name)
                .contains("John Doe");
    }

    @Test
    void testSorting() throws QueryNodeException, IOException {
        Assertions.assertThat(search.search("name:john OR name:max", "age", SortOrder.DESCENDING, 1, 10))
                .hasSize(3)
                .extracting(SearchableItem::age)
                .containsExactly(35, 30, 25);

        Assertions.assertThat(search.search("name:john OR name:max", "age", SortOrder.ASCENDING, 1, 10))
                .hasSize(3)
                .extracting(SearchableItem::age)
                .containsExactly(25, 30, 35);
    }

    @Test
    void testPaging() throws QueryNodeException, IOException {
        Assertions.assertThat(search.search("", "age", SortOrder.ASCENDING, 1, 10))
                .hasSize(5);

        Assertions.assertThat(search.search("", "age", SortOrder.ASCENDING, 2, 2))
                .hasSize(2)
                .extracting(SearchableItem::age)
                .containsExactly(35, 40);

        Assertions.assertThat(search.search("", "age", SortOrder.ASCENDING, 3, 2))
                .hasSize(1)
                .extracting(SearchableItem::age)
                .containsExactly(45);

    }

    private record SearchableItem(String name, int age) implements InMemorySearchableEntity {

        @Override
        public void buildLuceneDoc(LuceneDocBuilder builder) {
            builder.stringVal("name", name);
            builder.intVal("age", age);
        }
    }
}
