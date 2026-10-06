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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.AttributeSortSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
public class PaginatedIndexSetServiceTest {
    private static final String FIXTURE = "PaginatedIndexSetServiceTest.json";

    private static final String ID_ALPHA = "66a0000000000000000000a1";
    private static final String ID_BRAVO = "66a0000000000000000000a2";
    private static final String ID_CHARLIE = "66a0000000000000000000a3";
    private static final String ID_DELTA = "66a0000000000000000000a4";
    private static final String ID_ECHO = "66a0000000000000000000a5";
    private static final String ID_FOXTROT = "66a0000000000000000000a6";
    private static final String ID_GOLF = "66a0000000000000000000a7";
    private static final String ID_HOTEL = "66a0000000000000000000a8";
    private static final int FIXTURE_COUNT = 8;
    private static final Predicate<IndexSetConfig> ALLOW_ALL = config -> true;

    // Mirrors the category-relevant fields in PaginatedIndexSetServiceTest.json.
    private static final Map<IndexSetCategory, Set<String>> EXPECTED_IDS_BY_CATEGORY = Map.of(
            IndexSetCategory.USER, Set.of(ID_ALPHA, ID_BRAVO, ID_HOTEL),
            IndexSetCategory.SYSTEM, Set.of(ID_CHARLIE, ID_DELTA, ID_ECHO),
            IndexSetCategory.ILLUMINATE, Set.of(ID_FOXTROT, ID_GOLF)
    );

    private PaginatedIndexSetService service;
    private MongoCollections mongoCollections;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        service = new PaginatedIndexSetService(mongoCollections);
    }

    private static DbSortResolver.ResolvedSort plainSort(String field, SortOrder order) {
        return new DbSortResolver.ResolvedSort(List.of(), order.toBsonSort(field), List.of());
    }

    private static DbSortResolver.ResolvedSort lookupSort(AttributeSortSpec spec, SortOrder order) {
        return new DbSortResolver.ResolvedSort(spec.preSortStages(), order.toBsonSort(spec.sortField()), spec.postSortStages());
    }

    private static DbSortResolver.ResolvedSort titleAsc() {
        return plainSort(IndexSetConfig.FIELD_TITLE, SortOrder.ASCENDING);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void sortsByTitleAscendingIgnoringCase() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL, titleAsc(), 1, 0);

        assertThat(result).extracting(IndexSetConfig::title)
                .containsExactly("alpha", "Bravo", "charlie", "Delta", "echo", "foxtrot", "Golf", "hotel");
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void sortsByCreationDateDescending() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                plainSort(IndexSetConfig.FIELD_CREATION_DATE, SortOrder.DESCENDING), 1, 0);

        assertThat(result).extracting(IndexSetConfig::id)
                .containsExactly(ID_HOTEL, ID_GOLF, ID_FOXTROT, ID_ECHO, ID_DELTA, ID_CHARLIE, ID_BRAVO, ID_ALPHA);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void returnsRequestedPageWithFullTotal() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL, titleAsc(), 2, 2);

        assertThat(result).extracting(IndexSetConfig::id).containsExactly(ID_CHARLIE, ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
        assertThat(result.pagination().page()).isEqualTo(2);
        assertThat(result.pagination().perPage()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void appliesDbQuery() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(
                Filters.eq(IndexSetConfig.FIELD_INDEX_PREFIX, "idx_charlie"), ALLOW_ALL, titleAsc(), 1, 10);

        assertThat(result).extracting(IndexSetConfig::id).containsExactly(ID_CHARLIE);
        assertThat(result.pagination().total()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void predicateFiltersListAndTotalConsistently() {
        final Set<String> allowedIds = Set.of(ID_BRAVO, ID_DELTA);
        final Predicate<IndexSetConfig> onlyAllowed = config -> allowedIds.contains(config.id());

        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), onlyAllowed, titleAsc(), 1, 10);

        assertThat(result).extracting(IndexSetConfig::id).containsExactly(ID_BRAVO, ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void returnsEmptyPageBeyondRangeWithTotalUnchanged() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL, titleAsc(), 100, 2);

        assertThat(result).isEmpty();
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void pagesInTheDatabaseWithoutAPredicate() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), titleAsc(), 2, 2);

        assertThat(result.delegate()).extracting(IndexSetConfig::id).containsExactly(ID_CHARLIE, ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void categoryBsonMatchesJavaClassificationForEveryFixture() {
        for (final IndexSetCategory category : IndexSetCategory.values()) {
            final PaginatedList<IndexSetConfig> result = service.findPaginated(category.toBson(), ALLOW_ALL, titleAsc(), 1, 0);

            assertThat(result).extracting(IndexSetConfig::id)
                    .containsExactlyInAnyOrderElementsOf(EXPECTED_IDS_BY_CATEGORY.get(category));
            assertThat(result).allSatisfy(config -> assertThat(IndexSetCategory.of(config)).isEqualTo(category));
        }
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void countCategoriesCountsEveryCategory() {
        assertThat(service.countCategories()).isEqualTo(new IndexSetCategoryCounts(8, 3, 3, 2));
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void countCategoriesWithPredicateHonoursIt() {
        final Set<String> allowedIds = Set.of(ID_ALPHA, ID_DELTA);

        final IndexSetCategoryCounts counts = service.countCategories(config -> allowedIds.contains(config.id()));

        assertThat(counts).isEqualTo(new IndexSetCategoryCounts(2, 1, 1, 0));
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void sortsByProfileTitleThroughLookup() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                lookupSort(IndexSetAttributeSorts.profileTitle(), SortOrder.ASCENDING), 1, 0);

        // charlie uses "Alpha profile" and alpha uses "Zulu profile". Sets without a profile may land anywhere.
        assertThat(result).extracting(IndexSetConfig::id).containsSubsequence(ID_CHARLIE, ID_ALPHA);
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
        // The sort unsets its temporary fields, so every document still maps onto IndexSetConfig.
        assertThat(result).hasSize(FIXTURE_COUNT).allSatisfy(config -> assertThat(config.title()).isNotNull());
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void sortsByStreamCountThroughLookup() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                lookupSort(IndexSetAttributeSorts.streamCount(), SortOrder.DESCENDING), 1, 0);

        final List<String> ids = result.stream().map(IndexSetConfig::id).toList();
        assertThat(ids).hasSize(FIXTURE_COUNT);
        assertThat(ids.get(0)).isEqualTo(ID_BRAVO);
        assertThat(ids.get(1)).isEqualTo(ID_DELTA);
        assertThat(result.pagination().total()).isEqualTo(FIXTURE_COUNT);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void pagesOfTiedSortKeysNeitherOverlapNorSkip() {
        // Six fixtures have no streams and tie on stream count. Equal keys fall back to _id, so each page query
        // places them the same way. Without that fallback a tied set could appear on two pages or on none.
        final DbSortResolver.ResolvedSort sort = lookupSort(IndexSetAttributeSorts.streamCount(), SortOrder.DESCENDING);

        final List<String> ids = Stream.of(1, 2, 3)
                .flatMap(page -> service.findPaginated(Filters.empty(), ALLOW_ALL, sort, page, 3).stream())
                .map(IndexSetConfig::id)
                .toList();

        assertThat(ids).containsExactly(ID_BRAVO, ID_DELTA, ID_ALPHA, ID_CHARLIE, ID_ECHO, ID_FOXTROT, ID_GOLF, ID_HOTEL);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void keepsDirectionOfAnExplicitIdSort() {
        final PaginatedList<IndexSetConfig> result = service.findPaginated(Filters.empty(), ALLOW_ALL,
                plainSort("_id", SortOrder.DESCENDING), 1, 0);

        assertThat(result).extracting(IndexSetConfig::id)
                .containsExactly(ID_HOTEL, ID_GOLF, ID_FOXTROT, ID_ECHO, ID_DELTA, ID_CHARLIE, ID_BRAVO, ID_ALPHA);
    }

    @Test
    void fromValueRejectsUnknownCategory() {
        assertThat(IndexSetCategory.fromValue("illuminate")).isEqualTo(IndexSetCategory.ILLUMINATE);
        assertThatThrownBy(() -> IndexSetCategory.fromValue("nope")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @MongoDBFixtures(FIXTURE)
    void rotationModelPredicatesSplitTieredFromLegacyIndexSets() {
        // The test mapper lacks the Guice-registered data_tiering subtypes, so a fixture would load as the fallback
        // type and lose its subtype. A raw document keeps the stored shape exact for the predicates under test.
        final MongoCollection<Document> raw = mongoCollections.nonEntityCollection(MongoIndexSetService.COLLECTION_NAME, Document.class);
        final ObjectId tieredId = new ObjectId("66a0000000000000000000d1");
        raw.insertOne(new Document("_id", tieredId).append("title", "tiered")
                .append("data_tiering", new Document("type", "hot_only").append("index_lifetime_min", "P30D")));
        try {
            assertThat(raw.find(RotationModel.DATA_TIERING.toBson()).map(doc -> doc.getString("title"))).containsExactly("tiered");
            assertThat(raw.countDocuments(RotationModel.LEGACY.toBson())).isEqualTo(FIXTURE_COUNT);
        } finally {
            raw.deleteOne(new Document("_id", tieredId));
        }
    }

    @Test
    void rotationModelFromValueRejectsUnknownValues() {
        assertThat(RotationModel.fromValue("Legacy")).isEqualTo(RotationModel.LEGACY);
        assertThatThrownBy(() -> RotationModel.fromValue("nope")).isInstanceOf(IllegalArgumentException.class);
    }
}
