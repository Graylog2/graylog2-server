package org.graylog.plugins.enterprise.search.db;

import com.google.common.collect.Streams;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.SearchRequirements;
import org.graylog.plugins.enterprise.search.views.ViewService;
import org.graylog.plugins.enterprise.search.views.sharing.IsViewSharedForUser;
import org.graylog.plugins.enterprise.search.views.sharing.ViewSharingService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a helper to implement a basic Mongojack-based database service that allows CRUD operations on a single DTO type.
 *
 * <p>
 * Subclasses can add more sophisticated search methods by access the protected "db" property.<br/>
 * Indices can be added in the constructor.
 * </p>
 */
public class SearchDbService {
    protected final JacksonDBCollection<Search, ObjectId> db;
    private final ViewService viewService;
    private final ViewSharingService viewSharingService;
    private final IsViewSharedForUser isViewSharedForUser;
    private final SearchRequirements.Factory searchRequirementsFactory;

    @Inject
    protected SearchDbService(MongoConnection mongoConnection,
                              MongoJackObjectMapperProvider mapper,
                              ViewService viewService,
                              ViewSharingService viewSharingService,
                              IsViewSharedForUser isViewSharedForUser,
                              SearchRequirements.Factory searchRequirementsFactory) {
        this.viewService = viewService;
        this.viewSharingService = viewSharingService;
        this.isViewSharedForUser = isViewSharedForUser;
        this.searchRequirementsFactory = searchRequirementsFactory;
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("searches"),
                Search.class,
                ObjectId.class,
                mapper.get());
        db.createIndex(new BasicDBObject("created_at", 1), new BasicDBObject("unique", false));
    }

    public Optional<Search> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)))
                .map(this::requirementsForSearch);
    }

    public Optional<Search> getForUser(String id, User user, Predicate<String> permissionChecker) {
        final Optional<Search> search = get(id).map(this::requirementsForSearch);

        if (!search.isPresent()) {
            return Optional.empty();
        }

        if (search.map(s -> s.owner().map(owner -> owner.equals(user.getName())).orElse(false)).orElse(false)) {
            return search;
        }

        if (viewService.forSearch(id).stream()
                .map(view -> viewSharingService.forView(view.id()))
                .anyMatch(viewSharing -> viewSharing.map(sharing -> isViewSharedForUser.isAllowedToSee(user, sharing)).orElse(false))) {
            return search;
        }

        if (viewService.forSearch(id).stream()
                .anyMatch(view -> permissionChecker.test(view.id()))) {
            return search;
        }

        return Optional.empty();
    }

    public Search save(Search search) {
        final Search searchToSave = requirementsForSearch(search);
        if (searchToSave.id() != null) {
            db.update(
                    DBQuery.is("_id", search.id()),
                    searchToSave,
                    true,
                    false
            );

            return searchToSave;
        }

        final WriteResult<Search, ObjectId> save = db.insert(searchToSave);

        return save.getSavedObject();
    }

    public PaginatedList<Search> findPaginated(DBQuery.Query search, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<Search> cursor = db.find(search)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(
                Streams.stream((Iterable<Search>) cursor).map(this::requirementsForSearch).collect(Collectors.toList()),
                cursor.count(),
                page,
                perPage
        );
    }

    public void delete(String id) {
        db.removeById(new ObjectId(id));
    }

    public Collection<Search> findByIds(Set<String> idSet) {
        return Streams.stream((Iterable<Search>) db.find(DBQuery.in("_id", idSet.stream().map(ObjectId::new).collect(Collectors.toList()))))
                .map(this::requirementsForSearch)
                .collect(Collectors.toList());
    }

    public Stream<Search> streamAll() {
        return Streams.stream((Iterable<Search>) db.find()).map(this::requirementsForSearch);
    }

    private Search requirementsForSearch(Search search) {
        return searchRequirementsFactory.create(search)
                .rebuildRequirements(Search::requires, (s, newRequirements) -> s.toBuilder().requires(newRequirements).build());
    }
}
