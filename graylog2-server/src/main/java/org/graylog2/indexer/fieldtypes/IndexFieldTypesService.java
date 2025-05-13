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
package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.include;
import static org.graylog2.indexer.fieldtypes.FieldTypeDTO.FIELD_NAME;
import static org.graylog2.indexer.fieldtypes.FieldTypeDTO.FIELD_PHYSICAL_TYPE;
import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.TYPE_MAP;
import static org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO.FIELD_FIELDS;
import static org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO.FIELD_INDEX_NAME;
import static org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO.FIELD_INDEX_SET_ID;
import static org.graylog2.indexer.indexset.CustomFieldMappings.REVERSE_TYPES;

/**
 * Manages the "index_field_types" MongoDB collection.
 */
public class IndexFieldTypesService {
    private static final String FIELDS_FIELD_NAMES = String.format(Locale.US, "%s.%s", FIELD_FIELDS, FIELD_NAME);

    private final MongoCollection<IndexFieldTypesDTO> collection;
    private final MongoUtils<IndexFieldTypesDTO> mongoUtils;
    private final MongoCollection<Document> rawCollection;

    @Inject
    public IndexFieldTypesService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection("index_field_types", IndexFieldTypesDTO.class);
        mongoUtils = mongoCollections.utils(collection);
        rawCollection = collection.withDocumentClass(Document.class);

        collection.createIndex(ascending(FIELD_INDEX_NAME, FIELD_INDEX_SET_ID), new IndexOptions().unique(true));
        collection.createIndex(ascending(FIELD_INDEX_NAME), new IndexOptions().unique(true));
        collection.createIndex(ascending(FIELDS_FIELD_NAMES));
        collection.createIndex(ascending(FIELD_INDEX_SET_ID));
    }

    public List<String> fieldTypeHistory(final String indexSetId,
                                         final String fieldName,
                                         final boolean skipEntriesWithUnchangedType) {
        final AggregateIterable<Document> aggregateResult = this.rawCollection.aggregate(List.of(
                        Aggregates.unwind("$" + FIELD_FIELDS),
                        Aggregates.match(and(
                                eq(FIELD_INDEX_SET_ID, indexSetId),
                                eq(FIELD_FIELDS + "." + FIELD_NAME, fieldName)
                        )),
                        Aggregates.project(Projections.fields(
                                include(FIELD_FIELDS + "." + FIELD_PHYSICAL_TYPE),
                                excludeId()
                        ))
                )
        );

        List<String> typeHistory = new ArrayList<>();

        aggregateResult
                .map(document -> ((Document) document.get(FIELD_FIELDS)).getString(FIELD_PHYSICAL_TYPE))
                .map(TYPE_MAP::get)
                .map(REVERSE_TYPES::get)
                .forEach(typeHistory::add);

        if (!skipEntriesWithUnchangedType) {
            return typeHistory;
        } else {
            LinkedList<String> reducedTypeHistory = new LinkedList<>();
            typeHistory.forEach(type -> {
                if (reducedTypeHistory.isEmpty() || (type != null && !type.equals(reducedTypeHistory.getLast()))) {
                    reducedTypeHistory.add(type);
                }
            });
            return reducedTypeHistory;
        }
    }

    public Optional<IndexFieldTypesDTO> get(String idOrIndexName) {
        try {
            return mongoUtils.getById(new ObjectId(idOrIndexName));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            return Optional.ofNullable(collection.find(eq(FIELD_INDEX_NAME, idOrIndexName)).first());
        }
    }

    public IndexFieldTypesDTO save(IndexFieldTypesDTO dto) {
        return mongoUtils.save(dto);
    }

    public Optional<IndexFieldTypesDTO> upsert(IndexFieldTypesDTO dto) {
        final UpdateResult updateResult = collection.replaceOne(
                and(
                        eq(FIELD_INDEX_NAME, dto.indexName()),
                        eq(FIELD_INDEX_SET_ID, dto.indexSetId())
                ),
                dto,
                new ReplaceOptions().upsert(true)
        );

        final var id = updateResult.getUpsertedId();
        if (id != null) {
            return Optional.of(dto.toBuilder()
                    .id(id.asObjectId().getValue().toHexString())
                    .build());
        }

        return Optional.empty();
    }

    public void delete(String idOrIndexName) {
        try {
            mongoUtils.deleteById(new ObjectId(idOrIndexName));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            collection.deleteOne(eq(FIELD_INDEX_NAME, idOrIndexName));
        }
    }

    public Collection<IndexFieldTypesDTO> findForIndexSet(String indexSetId) {
        return findByQuery(eq(FIELD_INDEX_SET_ID, indexSetId));
    }

    public Collection<IndexFieldTypesDTO> findForIndexSets(Collection<String> indexSetIds) {
        return findByQuery(in(FIELD_INDEX_SET_ID, indexSetIds));
    }

    public Collection<IndexFieldTypesDTO> findForFieldNames(Collection<String> fieldNames) {
        return findByQuery(in(FIELDS_FIELD_NAMES, fieldNames));
    }

    public Collection<IndexFieldTypesDTO> findForFieldNamesAndIndices(Collection<String> fieldNames, Collection<String> indexNames) {
        final var query = and(
                in(FIELD_INDEX_NAME, indexNames),
                in(FIELDS_FIELD_NAMES, fieldNames)
        );

        return findByQuery(query);
    }

    public Collection<IndexFieldTypesDTO> findAll() {
        return findByQuery(Filters.empty());
    }

    private Collection<IndexFieldTypesDTO> findByQuery(Bson query) {
        return ImmutableList.copyOf(collection.find(query));
    }

    public IndexFieldTypesDTO findOneByIndexName(final String indexName) {
        return collection.find(eq(FIELD_INDEX_NAME, indexName)).first();
    }

    public List<IndexFieldTypesDTO> findByIndexNames(final Collection<String> indexNames) {
        return collection.find(in(FIELD_INDEX_NAME, indexNames)).into(new ArrayList<>());
    }
}
