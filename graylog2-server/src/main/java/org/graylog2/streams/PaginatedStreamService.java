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
package org.graylog2.streams;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.function.Predicate;

public class PaginatedStreamService extends PaginatedDbService<StreamDTO> {
    private static final String COLLECTION_NAME = "streams";

    @Inject
    public PaginatedStreamService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider mapper)
    {
        super(mongoConnection, mapper, StreamDTO.class, COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<StreamDTO> findPaginated(SearchQuery searchQuery, Predicate<StreamDTO> filter, int page,
                                                  int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryFilterAndSort(dbQuery, filter, sortBuilder, page, perPage);
    }
}
