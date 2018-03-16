package org.graylog.plugins.enterprise.database;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a helper to implement a basic Mongojack-based database service that allows CRUD operations on a
 * single DTO type and offers paginated access.
 * <p>
 *     It makes only a few assumptions, which are common to many Graylog entities:
 *     <ul>
 *         <li>The DTO class has a name which is unique</li>
 *     </ul>
 * </p>
 * <p>
 *     Subclasses can add more sophisticated query methods by access the protected "db" property.<br/>
 *     Indices can be added in the constructor.
 * </p>
 * @param <DTO>
 */
public abstract class PaginatedDbService<DTO> {
    protected final JacksonDBCollection<DTO, ObjectId> db;

    protected PaginatedDbService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper,
                                 Class<DTO> dtoClass,
                                 String collectionName) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(collectionName),
                dtoClass,
                ObjectId.class,
                mapper.get());
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
     */
    public void delete(String id) {
        db.removeById(new ObjectId(id));
    }

    /**
     * Returns a {@link PaginatedList<DTO>} for the given query and pagination parameters.
     * <p>
     * This method is only accessible by subclasses to avoid exposure of the {@link DBQuery} and {@link DBSort}
     * interfaces to consumers.
     *
     * @param query the query to execute
     * @param sort the sort builder for the query
     * @param page the page number that should be returned
     * @param perPage the number of entries per page
     * @return the paginated list
     */
    protected PaginatedList<DTO> findPaginatedWithQueryAndSort(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {
        final DBCursor<DTO> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
    }

    private ImmutableList<DTO> asImmutableList(Iterator<? extends DTO> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    /**
     * Returns an unordered stream of all entries in the database.
     *
     * @return stream of all database entries
     */
    public Stream<DTO> streamAll() {
        return streamQuery(DBQuery.empty());
    }

    /**
     * Returns an unordered stream of all entries in the database for the given IDs.
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
     *
     * @param query the query to execute
     * @return stream of database entries that match the query
     */
    protected Stream<DTO> streamQuery(DBQuery.Query query) {
        return Streams.stream((Iterable<DTO>) db.find(query));
    }

    /**
     * Returns a stream of database entries for the given {@link DBQuery.Query} sorted by the give {@link DBSort.SortBuilder}.
     *
     * @param query the query to execute
     * @param sort the sort order for the query
     * @return stream of database entries that match the query
     */
    protected Stream<DTO> streamQueryWithSort(DBQuery.Query query, DBSort.SortBuilder sort) {
        return Streams.stream((Iterable<DTO>) db.find(query).sort(sort));
    }
}
