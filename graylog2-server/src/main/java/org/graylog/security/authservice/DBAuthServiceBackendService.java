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
package org.graylog.security.authservice;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.PaginationParameters;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class DBAuthServiceBackendService extends PaginatedDbService<AuthServiceBackendDTO> {
    @Inject
    protected DBAuthServiceBackendService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, AuthServiceBackendDTO.class, "auth_service_backends");
    }

    public PaginatedList<AuthServiceBackendDTO> findPaginated(PaginationParameters params,
                                                              Predicate<AuthServiceBackendDTO> filter) {
        final String sortBy = defaultIfBlank(params.getSortBy(), "title");
        final DBSort.SortBuilder sortBuilder = getSortBuilder(params.getOrder(), sortBy);

        return findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, sortBuilder, params.getPage(), params.getPerPage());
    }
}
