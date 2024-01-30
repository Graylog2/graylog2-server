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
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoDBUpsertRetryer;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
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

    private final JacksonDBCollection<IndexFieldTypesDTO, ObjectId> db;
    private final MongoCollection<Document> mongoCollection;


    @Inject
    public IndexFieldTypesService(final MongoConnection mongoConnection,
                                  final MongoJackObjectMapperProvider objectMapperProvider) {
        this.mongoCollection = mongoConnection.getMongoDatabase().getCollection("index_field_types");
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("index_field_types"),
                IndexFieldTypesDTO.class,
                ObjectId.class,
                objectMapperProvider.get());

        this.db.createIndex(new BasicDBObject(ImmutableMap.of(
                FIELD_INDEX_NAME, 1,
                FIELD_INDEX_SET_ID, 1
        )), new BasicDBObject("unique", true));
        this.db.createIndex(new BasicDBObject(FIELD_INDEX_NAME, 1), new BasicDBObject("unique", true));
        this.db.createIndex(new BasicDBObject(FIELDS_FIELD_NAMES, 1));
        this.db.createIndex(new BasicDBObject(FIELD_INDEX_SET_ID, 1));
    }

    public List<String> fieldTypeHistory(final String indexSetId,
                                         final String fieldName,
                                         final boolean skipEntriesWithUnchangedType) {
        final AggregateIterable<Document> aggregateResult = this.mongoCollection.aggregate(List.of(
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
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrIndexName)));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            return Optional.ofNullable(db.findOne(DBQuery.is(FIELD_INDEX_NAME, idOrIndexName)));
        }
    }

    public IndexFieldTypesDTO save(IndexFieldTypesDTO dto) {
        final WriteResult<IndexFieldTypesDTO, ObjectId> save = db.save(dto);
        return save.getSavedObject();
    }

    public Optional<IndexFieldTypesDTO> upsert(IndexFieldTypesDTO dto) {
        final WriteResult<IndexFieldTypesDTO, ObjectId> update = MongoDBUpsertRetryer.run(() -> db.update(
                DBQuery.and(
                        DBQuery.is(FIELD_INDEX_NAME, dto.indexName()),
                        DBQuery.is(FIELD_INDEX_SET_ID, dto.indexSetId())
                ),
                dto,
                true,
                false
        ));

        final Object upsertedId = update.getUpsertedId();
        if (upsertedId instanceof ObjectId) {
            return get(((ObjectId) upsertedId).toHexString());
        } else if (upsertedId instanceof String) {
            return get((String) upsertedId);
        }
        return Optional.empty();
    }

    public void delete(String idOrIndexName) {
        try {
            db.removeById(new ObjectId(idOrIndexName));
        } catch (IllegalArgumentException e) {
            // Not an ObjectId, try again with index_name
            db.remove(DBQuery.is(FIELD_INDEX_NAME, idOrIndexName));
        }
    }

    public Collection<IndexFieldTypesDTO> findForIndexSet(String indexSetId) {
        return findByQuery(DBQuery.is(FIELD_INDEX_SET_ID, indexSetId));
    }

    public Collection<IndexFieldTypesDTO> findForIndexSets(Collection<String> indexSetIds) {
        return findByQuery(
                DBQuery.in(FIELD_INDEX_SET_ID, indexSetIds)
        );
    }

    public Collection<IndexFieldTypesDTO> findForFieldNames(Collection<String> fieldNames) {
        return findByQuery(DBQuery.in(FIELDS_FIELD_NAMES, fieldNames));
    }

    public Collection<IndexFieldTypesDTO> findForFieldNamesAndIndices(Collection<String> fieldNames, Collection<String> indexNames) {
        final DBQuery.Query query = DBQuery.and(
                DBQuery.in(FIELD_INDEX_NAME, indexNames),
                DBQuery.in(FIELDS_FIELD_NAMES, fieldNames)
        );

        return findByQuery(query);
    }

    public Collection<IndexFieldTypesDTO> findAll() {
        return findByQuery(DBQuery.empty());
    }

    private Collection<IndexFieldTypesDTO> findByQuery(DBQuery.Query query) {
        return ImmutableList.copyOf((Iterable<IndexFieldTypesDTO>) db.find(query));
    }

    public IndexFieldTypesDTO findOneByIndexName(final String indexName) {
        return db.findOne(DBQuery.is(FIELD_INDEX_NAME, indexName));
    }
}
