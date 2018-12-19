package org.graylog.plugins.enterprise.search.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.views.ViewService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a helper to implement a basic Mongojack-based database service that allows CRUD operations on a single DTO type.
 *
 * <p>
 *     Subclasses can add more sophisticated search methods by access the protected "db" property.<br/>
 *     Indices can be added in the constructor.
 * </p>
 */
public class SearchDbService {
    protected final JacksonDBCollection<Search, ObjectId> db;
    private final ViewService viewService;

    @Inject
    protected SearchDbService(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider mapper,
                              ViewService viewService) {
        this.viewService = viewService;
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                Search.class,
                ObjectId.class,
                mapper.get());
        db.createIndex(new BasicDBObject("created_at", 1), new BasicDBObject("unique", false));
    }

    public Optional<Search> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    public Optional<Search> getForUser(String id, String username, Predicate<String> permissionChecker) {
        final Search search = db.findOneById(new ObjectId(id));

        if (search == null) {
            return Optional.empty();
        }

        if (search.owner().map(owner -> owner.equals(username)).orElse(false)) {
            return Optional.of(search);
        }

        if (viewService.forSearch(id).stream()
                .anyMatch(view -> permissionChecker.test(view.id()))) {
            return Optional.of(search);
        }

        return Optional.empty();
    }

    public Search save(Search search) {
        if (search.id() != null) {
            final Search previous = db.findOneById(new ObjectId(search.id()));
            if (previous != null && previous.owner().map(owner -> !owner.equals(search.owner().orElse(null))).orElse(false)) {
                throw new IllegalArgumentException("Unable to update search with id <" + search.id() + ">, already exists and user is not permitted to overwrite it.");
            }

            final WriteResult<Search, ObjectId> save = db.update(
                    DBQuery.is("_id", search.id()),
                    search,
                    true,
                    false
            );

            return search;
        }

        final WriteResult<Search, ObjectId> save = db.insert(search);

        return save.getSavedObject();
    }

    public PaginatedList<Search> findPaginated(DBQuery.Query search, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<Search> cursor = db.find(search)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
    }

    private ImmutableList<Search> asImmutableList(Iterator<? extends Search> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    public void delete(String id) {
        db.removeById(new ObjectId(id));
    }

    public Collection<Search> findByIds(Set<String> idSet) {
        return asImmutableList(db.find(DBQuery.in("_id", idSet.stream().map(ObjectId::new).collect(Collectors.toList()))));
    }

    public Stream<Search> streamAll() {
        return Streams.stream((Iterable<Search>) db.find());
    }
}