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
package org.graylog2.indexer.indexset;

import com.mongodb.client.model.Filters;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
public class PaginatedIndexSetServiceTest {

    private static final String ID_ALPHA = "66a0000000000000000000a1";
    private static final String ID_BRAVO = "66a0000000000000000000a2";
    private static final String ID_CHARLIE = "66a0000000000000000000a3";
    private static final String ID_DELTA = "66a0000000000000000000a4";
    private static final String ID_ECHO = "66a0000000000000000000a5";
    private static final int FIXTURE_COUNT = 5;
    private static final Predicate<IndexSetConfig> ALLOW_ALL = config -> true;

    private PaginatedIndexSetService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        service = new PaginatedIndexSetService(mongoCollections);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void sortsByTitleAscendingIgnoringCase() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                1, 10, IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);

        assertThat(result).extracting(IndexSetConfig::title)
                .containsExactly("alpha", "Bravo", "charlie", "Delta", "echo");
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void sortsByCreationDateDescending() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                1, 10, IndexSetConfig.FIELD_CREATION_DATE, SortOrder.DESCENDING);

        assertThat(result).extracting(IndexSetConfig::id)
                .containsExactly(ID_ECHO, ID_DELTA, ID_CHARLIE, ID_BRAVO, ID_ALPHA);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void returnsRequestedPageWithFullTotal() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                2, 2, IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);

        assertThat(result).extracting(IndexSetConfig::id).containsExactly(ID_CHARLIE, ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
        assertThat(result.pagination().page()).isEqualTo(2);
        assertThat(result.pagination().perPage()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void appliesDbQuery() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(
                Filters.eq(IndexSetConfig.FIELD_INDEX_PREFIX, "idx_charlie"), ALLOW_ALL,
                1, 10, IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);

        assertThat(result).extracting(IndexSetConfig::id).containsExactly(ID_CHARLIE);
        assertThat(result.pagination().total()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void predicateFiltersListAndTotalConsistently() {
        final Set<String> allowedIds = Set.of(ID_BRAVO, ID_DELTA);
        final Predicate<IndexSetConfig> onlyAllowed = config -> allowedIds.contains(config.id());

        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), onlyAllowed,
                1, 10, IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);

        assertThat(result).extracting(IndexSetConfig::id).containsExactly(ID_BRAVO, ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void returnsEmptyPageBeyondRangeWithTotalUnchanged() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                100, 10, IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);

        assertThat(result).isEmpty();
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }

    @Test
    @MongoDBFixtures("PaginatedIndexSetServiceTest.json")
    void pagesInTheDatabaseWithoutAPredicate() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), 2, 2,
                IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);

        assertThat(result.delegate()).extracting(IndexSetConfig::id).containsExactly(ID_CHARLIE, ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }
}
