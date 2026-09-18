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
package org.graylog2.database.indices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.testing.ObjectMapperExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.pagination.DefaultMongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(ObjectMapperExtension.class)
class MongoDbIndexToolsTest {
    private static final String COLLECTION_NAME = "test";

    private MongoDbIndexTools toTest;
    private com.mongodb.client.MongoCollection<Document> db;
    private MongoCollection<Document> rawdb;

    @BeforeEach
    void setUp(MongoDBTestService mongodb, MongoCollections mongoCollections) {
        mongodb.mongoCollection(COLLECTION_NAME).drop();
        this.rawdb = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class);
        this.db = spy(this.rawdb);
        toTest = new MongoDbIndexTools(db);
    }

    @Test
    void throwsExceptionIfAnyStringSortFieldIsNotPresentOnTheListOfAllSortFields() {
        assertThrows(IllegalArgumentException.class, () -> toTest.prepareIndices("id", List.of("id", "number"), List.of("title")));
    }

    @Test
    void doesNotCreateIndexForId() {
        toTest.prepareIndices("id", List.of("id"), List.of());

        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void doesNotCreateSimpleIndexIfProperOneExists() {
        rawdb.createIndex(Indexes.ascending("number"));

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void doesNotCreateCollationIndexIfProperOneExists() {
        rawdb.createIndex(Indexes.ascending("summary"),
                new IndexOptions().collation(DefaultMongoPaginationHelper.DEFAULT_COLLATION_WITH_CASE_INSENSITIVE_SORTING));

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db, never()).createIndex(any());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void replacesLegacyCollationIndexWithCanonicalOne() {
        // Older deployments created collation indexes with just the locale, before the canonical
        // pagination collation grew strength/numericOrdering. Such indexes don't get used by
        // queries with the new collation, so they must be recreated.
        rawdb.createIndex(Indexes.ascending("summary"),
                new IndexOptions().collation(Collation.builder().locale("en").build()));

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).dropIndex(Indexes.ascending("summary"));
        verify(db).createIndex(eq(Indexes.ascending("summary")),
                argThat(indexOptions -> indexOptions.getCollation() != null
                        && indexOptions.getCollation().getLocale().equals("en")
                        && indexOptions.getCollation().getStrength() == CollationStrength.SECONDARY
                        && Boolean.TRUE.equals(indexOptions.getCollation().getNumericOrdering())));
    }

    @Test
    void createsSimpleIndexIfDoesNotExists() {
        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).createIndex(eq(Indexes.ascending("number")), argThat(indexOptions -> !indexOptions.isUnique()));
    }

    @Test
    void createsCollationIndexIfDoesNotExists() {
        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).createIndex(eq(Indexes.ascending("summary")), argThat(indexOptions -> indexOptions.getCollation().getLocale().equals("en")));
    }

    @Test
    void replacesWrongCollationIndexWithProperOne() {
        //number should not have collation index, but a simple one!
        rawdb.createIndex(Indexes.ascending("number"), new IndexOptions().collation(Collation.builder().locale("en").collationStrength(CollationStrength.TERTIARY).build()));

        toTest.prepareIndices("id", List.of("number"), List.of());
        verify(db).dropIndex(Indexes.ascending("number"));
        verify(db).createIndex(eq(Indexes.ascending("number")), argThat(indexOptions -> !indexOptions.isUnique()));
    }

    @Test
    void replacesWrongSimpleIndexWithProperOne() {
        //summary should not have collation index, but a simple one!
        rawdb.createIndex(Indexes.ascending("summary"));

        toTest.prepareIndices("id", List.of("summary"), List.of("summary"));
        verify(db).dropIndex(Indexes.ascending("summary"));
        verify(db).createIndex(eq(Indexes.ascending("summary")), argThat(indexOptions -> indexOptions.getCollation().getLocale().equals("en")));
    }

    @Test
    void createsTTLIndexIfDoesNotExist() {
        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(72), "updated_at");

        verify(db).createIndex(eq(Indexes.ascending("updated_at")),
                argThat(indexOptions -> Objects.equals(indexOptions.getExpireAfter(TimeUnit.SECONDS), 72L)));
    }

    @Test
    void doesNotTouchTTLIndexWithMatchingExpiry() {
        rawdb.createIndex(Indexes.ascending("updated_at"), new IndexOptions().expireAfter(72L, TimeUnit.SECONDS));

        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(72), "updated_at");

        verify(db, never()).dropIndex(any(Bson.class));
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void replacesTTLIndexWithChangedExpiry() {
        rawdb.createIndex(Indexes.ascending("updated_at"), new IndexOptions().expireAfter(72L, TimeUnit.SECONDS));

        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(3600), "updated_at");

        verify(db).dropIndex("updated_at_1");
        verify(db).createIndex(eq(Indexes.ascending("updated_at")),
                argThat(indexOptions -> Objects.equals(indexOptions.getExpireAfter(TimeUnit.SECONDS), 3600L)));
    }

    @Test
    void replacesPlainIndexWithTTLIndex() {
        // Deployments that created the index before it gained a TTL have a plain index on the field. Such an
        // index carries no expireAfterSeconds at all, so it must be replaced rather than kept.
        rawdb.createIndex(Indexes.ascending("updated_at"));

        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(72), "updated_at");

        verify(db).dropIndex("updated_at_1");
        verify(db).createIndex(eq(Indexes.ascending("updated_at")),
                argThat(indexOptions -> Objects.equals(indexOptions.getExpireAfter(TimeUnit.SECONDS), 72L)));
    }

    @Test
    void ignoresCompoundIndexesContainingTheTtlField() {
        // A compound index that merely happens to include the field is not the TTL index: it must be left
        // alone, and it must not be mistaken for an existing TTL index on the field.
        rawdb.createIndex(new Document("type", 1).append("updated_at", -1));

        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(72), "updated_at");

        assertThat(indexNames()).contains("type_1_updated_at_-1");
        assertThat(ttlSecondsOf("updated_at_1")).isEqualTo(72L);
    }

    @Test
    void replacesDescendingPlainIndexWithTTLIndex() {
        // The field may already carry a descending index, which is a different key spec than the ascending
        // one a TTL index is created with. It still has to be recognized and replaced.
        rawdb.createIndex(Indexes.descending("updated_at"));

        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(72), "updated_at");

        assertThat(indexNames()).doesNotContain("updated_at_-1");
        assertThat(ttlSecondsOf("updated_at_1")).isEqualTo(72L);
    }

    @Test
    void keepsDescendingTTLIndexWithMatchingExpiry() {
        // Direction is irrelevant for a single-field index, so a matching TTL is left in place rather than
        // rebuilt on every boot.
        rawdb.createIndex(Indexes.descending("updated_at"), new IndexOptions().expireAfter(72L, TimeUnit.SECONDS));

        MongoDbIndexTools.ensureTTLIndex(db, Duration.ofSeconds(72), "updated_at");

        verify(db, never()).dropIndex(anyString());
        verify(db, never()).createIndex(any(Bson.class), any(IndexOptions.class));
        assertThat(ttlSecondsOf("updated_at_-1")).isEqualTo(72L);
    }

    private List<String> indexNames() {
        try (final var stream = MongoUtils.stream(rawdb.listIndexes())) {
            return stream.map(index -> index.getString("name")).toList();
        }
    }

    private Long ttlSecondsOf(String indexName) {
        try (final var stream = MongoUtils.stream(rawdb.listIndexes())) {
            return stream
                    .filter(index -> indexName.equals(index.getString("name")))
                    .map(index -> index.get("expireAfterSeconds", Number.class))
                    .map(expireAfterSeconds -> expireAfterSeconds == null ? null : expireAfterSeconds.longValue())
                    .findFirst()
                    .orElse(null);
        }
    }
}
