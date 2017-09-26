package org.graylog.plugins.enterprise.search.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.rest.models.PaginatedList;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a helper to implement a basic Mongojack-based database service that allows CRUD operations on a single DTO type.
 *
 * <p>
 *     Subclasses can add more sophisticated query methods by access the protected "db" property.<br/>
 *     Indices can be added in the constructor.
 * </p>
 */
public class QueryDbService {
    protected final JacksonDBCollection<Query, ObjectId> db;

    @Inject
    @SuppressWarnings("unchecked")
    protected QueryDbService(MongoConnection mongoConnection,
                             MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("queries"),
                Query.class,
                ObjectId.class,
                mapper.get());
        db.createIndex(new BasicDBObject("created_at", 1), new BasicDBObject("unique", false));
    }

    public Optional<Query> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    public Query save(Query query) {
        WriteResult<Query, ObjectId> save = db.save(query);
        return save.getSavedObject();
    }

    public PaginatedList<Query> findPaginated(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<Query> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
    }

    private ImmutableList<Query> asImmutableList(Iterator<? extends Query> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    public void delete(String id) {
        db.removeById(new ObjectId(id));
    }

    public Collection<Query> findByIds(Set<String> idSet) {
        return asImmutableList(db.find(DBQuery.in("_id", idSet.stream().map(ObjectId::new).collect(Collectors.toList()))));
    }

    public Stream<Query> streamAll() {
        return Streams.stream((Iterable<Query>) db.find());
    }
}