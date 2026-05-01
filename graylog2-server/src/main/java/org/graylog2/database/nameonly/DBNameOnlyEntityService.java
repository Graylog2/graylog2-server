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
package org.graylog2.database.nameonly;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;
import org.bson.conversions.Bson;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stream;

/**
 * Persistence base for "name-only" entities — Mongo-backed lists whose only domain attribute is a
 * single string value (plus the synthetic {@code _id}). Concrete subclasses bind a collection name,
 * the entity class, and the value-field name; the base provides the standard CRUD + paginated listing
 * surface and enforces a unique index on the value field.
 */
public abstract class DBNameOnlyEntityService<T extends BuildableMongoEntity<T, B>, B extends BuildableMongoEntity.Builder<T, B>> {

    protected final MongoCollection<T> collection;
    protected final MongoUtils<T> mongoUtils;
    protected final MongoPaginationHelper<T> paginationHelper;
    protected final SearchQueryParser searchQueryParser;
    protected final String valueField;

    protected DBNameOnlyEntityService(MongoCollections mongoCollections,
                                      String collectionName,
                                      Class<T> entityClass,
                                      String valueField) {
        this.collection = mongoCollections.collection(collectionName, entityClass);
        this.mongoUtils = mongoCollections.utils(collection);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.valueField = valueField;
        this.searchQueryParser = new SearchQueryParser(valueField,
                ImmutableMap.of(valueField, SearchQueryField.create(valueField)));

        collection.createIndex(new BasicDBObject(valueField, 1), new IndexOptions().unique(true));
    }

    public Optional<T> get(String id) {
        return mongoUtils.getById(id);
    }

    public T save(T entity) {
        if (entity.id() != null) {
            collection.replaceOne(MongoUtils.idEq(entity.id()), entity);
            return entity;
        }
        final String id = insertedIdAsString(collection.insertOne(entity));
        return entity.toBuilder().id(id).build();
    }

    public long delete(String id) {
        return collection.deleteOne(MongoUtils.idEq(id)).getDeletedCount();
    }

    public PaginatedList<T> findPaginated(String query, int page, int perPage, SortOrder order,
                                          String sortByField, Predicate<T> filter) {
        final Bson searchQuery = searchQueryParser.parse(query).toBson();
        final Bson sort = order.toBsonSort(sortByField);
        return filter == null
                ? paginationHelper.filter(searchQuery).sort(sort).perPage(perPage).page(page)
                : paginationHelper.filter(searchQuery).sort(sort).perPage(perPage).page(page, filter);
    }

    public Optional<T> getByValue(String value) {
        return Optional.ofNullable(collection.find(eq(valueField, value)).first());
    }

    public T update(String id, String value) {
        return collection.findOneAndUpdate(MongoUtils.idEq(id), set(valueField, value));
    }

    @MustBeClosed
    public Stream<T> streamAll() {
        return stream(collection.find());
    }
}
