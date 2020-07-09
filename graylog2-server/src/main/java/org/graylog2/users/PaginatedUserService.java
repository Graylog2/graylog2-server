package org.graylog2.users;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;

public class PaginatedUserService extends PaginatedDbService<UserDTO> {
    private static final String COLLECTION_NAME = "users";

    @Inject
    public PaginatedUserService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, UserDTO.class, COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<UserDTO> findPaginated(SearchQuery searchQuery, int page,
                                                int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }
}
