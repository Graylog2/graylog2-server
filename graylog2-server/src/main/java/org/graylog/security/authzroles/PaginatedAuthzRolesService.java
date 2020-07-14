package org.graylog.security.authzroles;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.users.UserService;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

public class PaginatedAuthzRolesService extends PaginatedDbService<AuthzRoleDTO> {
    private static final String COLLECTION_NAME = "roles";

    private UserService userService;

    @Inject
    public PaginatedAuthzRolesService(MongoConnection mongoConnection,
                                      UserService userService,
                                      MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, AuthzRoleDTO.class, COLLECTION_NAME);
        this.userService = userService;
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<AuthzRoleDTO> findPaginated(SearchQuery searchQuery, int page,
                                                     int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<AuthzRoleDTO> findPaginatedForUser(SearchQuery searchQuery, int page,
                                                     int perPage, String sortField, String order, String username) {
        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException("Could not find user: " + username);
        }
        final DBQuery.Query dbQuery = searchQuery.toDBQuery()
                .in("_id", user.getRoleIds());
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }
}
