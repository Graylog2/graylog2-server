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
package org.graylog2.database;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a helper to implement a basic Mongojack-based database service that allows CRUD operations on a
 * single DTO type and offers paginated access.
 * <p>
 * It makes only a few assumptions, which are common to many Graylog entities:
 *     <ul>
 *         <li>The DTO class has a name which is unique</li>
 *     </ul>
 * </p>
 * <p>
 *     Subclasses can add more sophisticated query methods by access the protected "db" property.<br/>
 *     Indices can be added in the constructor.
 * </p>
 *
 * @param <DTO>
 */
public abstract class PaginatedDbService<DTO> {
    protected final JacksonDBCollection<DTO, ObjectId> db;

    protected PaginatedDbService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper,
                                 Class<DTO> dtoClass,
                                 String collectionName) {
        this(mongoConnection, mapper, dtoClass, collectionName, null);
    }

    protected PaginatedDbService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper,
                                 Class<DTO> dtoClass,
                                 String collectionName,
                                 Class<?> view) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(collectionName),
                dtoClass,
                ObjectId.class,
                mapper.get(),
                view);
    }

    /**
     * Get the {@link DTO} for the given ID.
     *
     * @param id the ID of the object
     * @return an Optional containing the found object or an empty Optional if no object can be found for the given ID
     */
    public Optional<DTO> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    /**
     * Stores the given {@link DTO} in the database.
     *
     * @param dto the {@link DTO} to save
     * @return the newly saved {@link DTO}
     */
    public DTO save(DTO dto) {
        final WriteResult<DTO, ObjectId> save = db.save(dto);
        return save.getSavedObject();
    }

    /**
     * Deletes the {@link DTO} for the given ID from the database.
     *
     * @param id ID of the {@link DTO} to delete
     * @return the number of deleted documents
     */
    public int delete(String id) {
        return db.removeById(new ObjectId(id)).getN();
    }

    /**
     * Returns a {@link PaginatedList<DTO>} for the given query and pagination parameters.
     * <p>
     * This method is only accessible by subclasses to avoid exposure of the {@link DBQuery} and {@link DBSort}
     * interfaces to consumers.
     *
     * @param query   the query to execute
     * @param sort    the sort builder for the query
     * @param page    the page number that should be returned
     * @param perPage the number of entries per page, 0 is unlimited
     * @return the paginated list
     */
    protected PaginatedList<DTO> findPaginatedWithQueryAndSort(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {
        try (final DBCursor<DTO> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1))) {
            final long grandTotal = db.count();
            return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage, grandTotal);
        }
    }

    protected ImmutableList<DTO> asImmutableList(Iterator<? extends DTO> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    /**
     * Returns a {@link PaginatedList<DTO>} for the given query, filter and pagination parameters.
     * <p>
     * Since the database cannot execute the filter function directly, this method streams over the result cursor
     * and executes the filter function for each database object. <strong>This increases memory consumption and should only be
     * used if necessary.</strong> Use the
     * {@link PaginatedDbService#findPaginatedWithQueryFilterAndSort(DBQuery.Query, Predicate, DBSort.SortBuilder, int, int) #findPaginatedWithQueryAndSort()}
     * method if possible.
     * <p>
     * This method is only accessible by subclasses to avoid exposure of the {@link DBQuery} and {@link DBSort}
     * interfaces to consumers.
     *
     * @param query   the query to execute
     * @param filter  the filter to apply to each database entry
     * @param sort    the sort builder for the query
     * @param page    the page number that should be returned
     * @param perPage the number of entries per page, 0 is unlimited
     * @return the paginated list
     */
    protected PaginatedList<DTO> findPaginatedWithQueryFilterAndSort(DBQuery.Query query,
                                                                     Predicate<DTO> filter,
                                                                     DBSort.SortBuilder sort,
                                                                     int page,
                                                                     int perPage) {
        // Calculate the total amount of items matching the query/filter, but before pagination
        final long total;
        try (final Stream<DTO> cursor = streamQueryWithSort(query, sort)) {
            total = cursor.filter(filter).count();
        }

        // Then create another filtered stream and only collect the entries according to page and perPage
        try (final Stream<DTO> resultStream = streamQueryWithSort(query, sort)) {
            Stream<DTO> filteredResultStream = resultStream.filter(filter);
            if (perPage > 0) {
                filteredResultStream = filteredResultStream.skip(perPage * Math.max(0, page - 1)).limit(perPage);
            }

            final long grandTotal = db.count();

            return new PaginatedList<>(filteredResultStream.collect(Collectors.toList()), Math.toIntExact(total), page, perPage, grandTotal);
        }
    }

    /**
     * Returns an unordered stream of all entries in the database.
     * <p>
     * The returned stream needs to be closed to free the underlying database resources.
     *
     * @return stream of all database entries
     */
    public Stream<DTO> streamAll() {
        return streamQuery(DBQuery.empty());
    }

    /**
     * Returns an unordered stream of all entries in the database for the given IDs.
     * <p>
     * The returned stream needs to be closed to free the underlying database resources.
     *
     * @param idSet set of IDs to query
     * @return stream of database entries for the given IDs
     */
    public Stream<DTO> streamByIds(Set<String> idSet) {
        final List<ObjectId> objectIds = idSet.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());

        return streamQuery(DBQuery.in("_id", objectIds));
    }

    /**
     * Returns an unordered stream of database entries for the given {@link DBQuery.Query}.
     * <p>
     * The returned stream needs to be closed to free the underlying database resources.
     *
     * @param query the query to execute
     * @return stream of database entries that match the query
     */
    protected Stream<DTO> streamQuery(DBQuery.Query query) {
        final DBCursor<DTO> cursor = db.find(query);
        return Streams.stream((Iterable<DTO>) cursor).onClose(cursor::close);
    }

    /**
     * Returns a stream of database entries for the given {@link DBQuery.Query} sorted by the give {@link DBSort.SortBuilder}.
     * <p>
     * The returned stream needs to be closed to free the underlying database resources.
     *
     * @param query the query to execute
     * @param sort  the sort order for the query
     * @return stream of database entries that match the query
     */
    protected Stream<DTO> streamQueryWithSort(DBQuery.Query query, DBSort.SortBuilder sort) {
        final DBCursor<DTO> cursor = db.find(query).sort(sort);
        return Streams.stream((Iterable<DTO>) cursor).onClose(cursor::close);
    }

    /**
     * Returns a sort builder for the given order and field name.
     *
     * @param order the order. either "asc" or "desc"
     * @param field the field to sort on
     * @return the sort builder
     */
    protected DBSort.SortBuilder getSortBuilder(String order, String field) {
        DBSort.SortBuilder sortBuilder;
        if ("desc".equalsIgnoreCase(order)) {
            sortBuilder = DBSort.desc(field);
        } else {
            sortBuilder = DBSort.asc(field);
        }
        return sortBuilder;
    }
}
