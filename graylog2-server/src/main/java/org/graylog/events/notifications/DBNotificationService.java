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
package org.graylog.events.notifications;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;

public class DBNotificationService extends PaginatedDbService<NotificationDto> {
    private static final String NOTIFICATION_COLLECTION_NAME = "event_notifications";

    @Inject
    public DBNotificationService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, NotificationDto.class, NOTIFICATION_COLLECTION_NAME);
    }

    public PaginatedList<NotificationDto> getAllPaginated(String sortByField, int page, int perPage) {
        return findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.asc(sortByField), page, perPage);
    }

    public PaginatedList<NotificationDto> getAllPaginated(SearchQuery query, String sortByField, int page, int perPage) {
        return findPaginatedWithQueryAndSort(query.toDBQuery(), DBSort.asc(sortByField), page, perPage);
    }
}
