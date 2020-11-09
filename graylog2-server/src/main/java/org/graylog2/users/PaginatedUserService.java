/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.users;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.http.util.TextUtils.isBlank;

public class PaginatedUserService extends PaginatedDbService<UserOverviewDTO> {
    private static final String COLLECTION_NAME = "users";

    @Inject
    public PaginatedUserService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, UserOverviewDTO.class, COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<UserOverviewDTO> findPaginated(SearchQuery searchQuery, int page,
                                                        int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByUserId(SearchQuery searchQuery, int page,
                                                                int perPage, String sortField, String order,
                                                                Set<String> userIds) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery()
                .in("_id", userIds);
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByRole(SearchQuery searchQuery, int page,
                                                              int perPage, String sortField, String order,
                                                              Set<String> roleIds) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery()
                .in(UserImpl.ROLES, roleIds);
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByAuthServiceBackend(SearchQuery searchQuery,
                                                                            int page,
                                                                            int perPage,
                                                                            String sortField,
                                                                            String order,
                                                                            String authServiceBackendId) {
        checkArgument(!isBlank(authServiceBackendId), "authServiceBackendId cannot be blank");

        final DBQuery.Query query = DBQuery.and(
                DBQuery.is(UserImpl.AUTH_SERVICE_ID, Optional.of(authServiceBackendId)),
                searchQuery.toDBQuery()
        );
        return findPaginatedWithQueryAndSort(query, getSortBuilder(order, sortField), page, perPage);
    }
}
