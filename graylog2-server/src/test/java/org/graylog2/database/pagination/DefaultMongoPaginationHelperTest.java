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
package org.graylog2.database.pagination;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class DefaultMongoPaginationHelperTest {

    private record DTO(String id, String name) implements MongoEntity {}

    // 16 documents in the collection
    private final List<DTO> DTOs =
            Stream.of("A", "B", "C", "D", "E", "F", "G", "H", "a", "b", "c", "d", "e", "f", "g", "h")
                    .map(name -> new DTO(new ObjectId().toHexString(), name))
                    .toList();

    private MongoCollections mongoCollections;
    private MongoCollection<DTO> collection;
    private MongoPaginationHelper<DTO> paginationHelper;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        collection = mongoCollections.collection("test", DTO.class);
        paginationHelper = new DefaultMongoPaginationHelper<>(collection);

        collection.insertMany(DTOs);
    }

    @Test
    void testFilter() {
        final Bson filter = Filters.in("name", "A", "B", "C");
        final PaginatedList<DTO> filterdPage = paginationHelper.filter(filter).page(1);
        assertThat(filterdPage)
                .isEqualTo(paginationHelper.filter(filter).page(1, alwaysTrue()))
                .containsExactlyElementsOf(DTOs.subList(0, 3));

        assertThat(filterdPage.pagination()).satisfies(pagination -> {
            assertThat(pagination.total()).isEqualTo(3);
            assertThat(pagination.page()).isEqualTo(1);
            assertThat(pagination.perPage()).isEqualTo(0);
        });
    }

    @Test
    void testSort() {
        assertThat(paginationHelper.sort(ascending("_id")).page(1))
                .isEqualTo(paginationHelper.sort(ascending("_id")).page(1, alwaysTrue()))
                .isEqualTo(paginationHelper.sort(SortOrder.ASCENDING.toBsonSort("_id")).page(1))
                .isEqualTo(paginationHelper.sort(SortOrder.ASCENDING.toBsonSort("_id")).page(1, alwaysTrue()))
                .containsExactlyElementsOf(DTOs);

        assertThat(paginationHelper.sort(ascending("name")).page(1))
                .isEqualTo(paginationHelper.sort(ascending("name")).page(1, alwaysTrue()))
                .isEqualTo(paginationHelper.sort(SortOrder.ASCENDING.toBsonSort("name")).page(1))
                .isEqualTo(paginationHelper.sort(SortOrder.ASCENDING.toBsonSort("name")).page(1, alwaysTrue()))
                .containsExactlyElementsOf(DTOs);

        assertThat(paginationHelper.sort(descending("_id")).page(1))
                .isEqualTo(paginationHelper.sort(descending("_id")).page(1, alwaysTrue()))
                .isEqualTo(paginationHelper.sort(SortOrder.DESCENDING.toBsonSort("_id")).page(1))
                .isEqualTo(paginationHelper.sort(SortOrder.DESCENDING.toBsonSort("_id")).page(1, alwaysTrue()))
                .containsExactlyElementsOf(Lists.reverse(DTOs));

        assertThat(paginationHelper.sort(descending("name")).page(1))
                .isEqualTo(paginationHelper.sort(descending("name")).page(1, alwaysTrue()))
                .isEqualTo(paginationHelper.sort(SortOrder.DESCENDING.toBsonSort("name")).page(1))
                .isEqualTo(paginationHelper.sort(SortOrder.DESCENDING.toBsonSort("name")).page(1, alwaysTrue()))
                .containsExactlyElementsOf(Lists.reverse(DTOs));
    }

    @Test
    void testProjection() {
        final Bson filter = Filters.in("name", "A", "B", "C");
        PaginatedList<DTO> page = paginationHelper.filter(filter)
                .projection(Projections.excludeId())
                .sort(ascending("name"))
                .perPage(5)
                .page(1);

        assertThat(page)
                .hasSize(3)
                .containsExactly(
                        new DTO(null, "A"),
                        new DTO(null, "B"),
                        new DTO(null, "C")
                );

        page = paginationHelper.filter(filter)
                .projection(Projections.exclude("name"))
                .sort(ascending("name"))
                .perPage(5)
                .page(1);

        assertThat(page)
                .hasSize(3)
                .allSatisfy((Consumer<DTO>) dto -> assertThat(dto.name()).isNull());
    }

    @Test
    void testPerPage() {
        assertThat(paginationHelper.page(1))
                .isEqualTo(paginationHelper.perPage(0).page(1))
                .isEqualTo(paginationHelper.perPage(0).page(1, alwaysTrue()))
                .containsExactlyElementsOf(DTOs);

        final MongoPaginationHelper<DTO> helper = paginationHelper.perPage(8);

        assertThat(helper.page(1)).containsExactlyElementsOf(DTOs.subList(0, 8));
        assertThat(helper.page(1, alwaysTrue())).containsExactlyElementsOf(DTOs.subList(0, 8));
        assertThat(helper.page(2)).containsExactlyElementsOf(DTOs.subList(8, 16));
        assertThat(helper.page(2, alwaysTrue())).containsExactlyElementsOf(DTOs.subList(8, 16));

        assertThat(helper.page(1).pagination()).satisfies(pagination -> {
            assertThat(pagination.total()).isEqualTo(16);
            assertThat(pagination.page()).isEqualTo(1);
            assertThat(pagination.perPage()).isEqualTo(8);
        });
    }

    @Test
    void testIncludeGrandTotal() {
        assertThat(paginationHelper.page(1).grandTotal())
                .isEqualTo(paginationHelper.includeGrandTotal(false).page(1).grandTotal())
                .isEqualTo(paginationHelper.includeGrandTotal(false).page(1, alwaysTrue()).grandTotal())
                .isEmpty();

        assertThat(paginationHelper.includeGrandTotal(true).page(1).grandTotal())
                .isEqualTo(paginationHelper.includeGrandTotal(true).page(1, alwaysTrue()).grandTotal())
                .contains(16L);
    }

    @Test
    void testGrandTotalFilter() {
        final Bson filter = Filters.in("name", "A", "B", "C");
        assertThat(paginationHelper.includeGrandTotal(true).grandTotalFilter(filter).page(1).grandTotal())
                .isEqualTo(paginationHelper.includeGrandTotal(true).grandTotalFilter(filter).page(1, alwaysTrue()).grandTotal())
                .contains(3L);
    }

    @Test
    void testWithSelector() {
        final Bson filter = Filters.in("name", "A", "B", "C", "a", "b", "c");
        final Predicate<DTO> selector = dto -> dto.name().equalsIgnoreCase("a");
        final MongoPaginationHelper<DTO> helper = paginationHelper
                .filter(filter)
                .sort(descending("_id"))
                .includeGrandTotal(true)
                .perPage(1);

        assertThat(helper.page(1, selector)).containsExactly(DTOs.get(8)); // "a"
        assertThat(helper.page(2, selector)).containsExactly(DTOs.get(0)); // "A"

        assertThat(helper.page(2, selector).pagination()).satisfies(pagination -> {
            assertThat(pagination.total()).isEqualTo(2);
            assertThat(pagination.page()).isEqualTo(2);
            assertThat(pagination.perPage()).isEqualTo(1);
        });
        assertThat(helper.page(2, selector).grandTotal()).contains(16L);
    }

    @Test
    void testCollation() {
        final Collation upperFirstCollation = Collation.builder().locale("en").collationCaseFirst(CollationCaseFirst.UPPER).build();
        final Comparator<String> upperFirstComparator = (a, b) -> a.toLowerCase(ENGLISH).equals(b.toLowerCase(ENGLISH)) ?
                a.compareTo(b) :
                a.toLowerCase(ENGLISH).compareTo(b.toLowerCase(ENGLISH));

        assertThat(paginationHelper.collation(upperFirstCollation).sort(ascending("name")).page(1))
                .containsExactlyElementsOf(DTOs.stream().sorted(Comparator.comparing(DTO::name, upperFirstComparator)).toList());
    }

}
