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

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.utils.MongoUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MongoDbIndexTools<T> {

    static final String COLLATION_KEY = "collation";
    static final String INDEX_DOCUMENT_KEY = "key";

    private final MongoCollection<T> db;

    public MongoDbIndexTools(final MongoCollection<T> db) {
        this.db = db;
    }

    // MongoDB Indexes cannot be altered once created.
    public static void ensureTTLIndex(MongoCollection<Document> collection, Duration ttl, String fieldUpdatedAt) {
        final IndexOptions indexOptions = new IndexOptions().expireAfter(ttl.toSeconds(), TimeUnit.SECONDS);
        final Bson updatedAtKey = Indexes.ascending(fieldUpdatedAt);
        for (Document document : collection.listIndexes()) {
            final Set<String> keySet = document.get(INDEX_DOCUMENT_KEY, Document.class).keySet();
            if (keySet.contains(fieldUpdatedAt)) {
                // Since MongoDB 5.0 this is an Integer. Used to be a Long ¯\_(ツ)_/¯
                final long expireAfterSeconds = document.get("expireAfterSeconds", Number.class).longValue();
                if (Objects.equals(expireAfterSeconds, indexOptions.getExpireAfter(TimeUnit.SECONDS))) {
                    return;
                }
                collection.dropIndex(updatedAtKey);
            }
        }
        // not found or dropped, creating new index
        collection.createIndex(updatedAtKey, indexOptions);
    }

    /**
     * Prepares indices in MongoDB collection, given proper document fields identifiers.
     *
     * @param idField                         Name of id field.
     * @param sortFields                      List of all sortable fields, which need index creation.
     * @param caseInsensitiveStringSortFields List of those of sortable fields than need case in-sensitive sorting.
     */
    public void prepareIndices(final String idField, final Collection<String> sortFields, final Collection<String> caseInsensitiveStringSortFields) {
        if (!sortFields.containsAll(caseInsensitiveStringSortFields)) {
            throw new IllegalArgumentException("Case Insensitive String Sort Fields should be a subset of all Sort Fields ");
        }
        final var existingIndices = db.listIndexes();
        for (String sortField : sortFields) {
            if (!sortField.equals(idField)) { //id has index by default
                final var existingIndex = getExistingIndex(existingIndices, sortField);
                if (caseInsensitiveStringSortFields.contains(sortField)) { //index string fields with collation for more efficient case-insensitive sorting
                    if (existingIndex.isEmpty()) {
                        createCaseInsensitiveStringIndex(sortField);
                    } else if (existingIndex.get().get(COLLATION_KEY) == null) {
                        //replace simple index with "collation" index
                        dropIndex(sortField);
                        createCaseInsensitiveStringIndex(sortField);
                    }
                } else {
                    if (existingIndex.isEmpty()) {
                        createSingleFieldIndex(sortField);
                    } else if (existingIndex.get().get(COLLATION_KEY) != null) {
                        //replace "collation" index with simple one
                        dropIndex(sortField);
                        createSingleFieldIndex(sortField);
                    }
                }
            }
        }
    }

    private void dropIndex(final String sortField) {
        this.db.dropIndex(Indexes.ascending(sortField));
    }

    private void createSingleFieldIndex(final String sortField) {
        this.db.createIndex(Indexes.ascending(sortField), new IndexOptions().unique(false));
    }

    private void createCaseInsensitiveStringIndex(final String sortField) {
        this.db.createIndex(Indexes.ascending(sortField), new IndexOptions().collation(Collation.builder().locale("en").build()));
    }

    private Optional<Document> getExistingIndex(final ListIndexesIterable<Document> existingIndices, final String sortField) {
        if (existingIndices == null) {
            return Optional.empty();
        }
        return MongoUtils.stream(existingIndices)
                .filter(info ->
                        info.get(INDEX_DOCUMENT_KEY, Document.class).containsKey(sortField)
                )
                .findFirst();
    }

    public void createUniqueIndex(final String field) {
        this.db.createIndex(Indexes.ascending(field), new IndexOptions().unique(true));
    }
}
