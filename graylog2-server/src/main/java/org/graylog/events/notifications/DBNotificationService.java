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

import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;

import javax.inject.Inject;
import java.util.function.Predicate;

public class DBNotificationService extends PaginatedDbService<NotificationDto> {
    private static final String NOTIFICATION_COLLECTION_NAME = "event_notifications";

    private final EntityOwnershipService entityOwnerShipService;

    @Inject
    public DBNotificationService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper,
                                 EntityOwnershipService entityOwnerShipService) {
        super(mongoConnection, mapper, NotificationDto.class, NOTIFICATION_COLLECTION_NAME);
        this.entityOwnerShipService = entityOwnerShipService;
    }

    public PaginatedList<NotificationDto> searchPaginated(SearchQuery query, Predicate<NotificationDto> filter,
                                                          String sortByField, int page, int perPage) {
        return findPaginatedWithQueryFilterAndSort(query.toDBQuery(), filter,
                getSortBuilder("asc", sortByField), page, perPage);
    }

    public NotificationDto saveWithOwnership(NotificationDto notificationDto, User user) {
        final NotificationDto dto = super.save(notificationDto);
        entityOwnerShipService.registerNewEventNotification(dto.id(), user);
        return dto;
    }

    @Override
    public int delete(String id) {
        entityOwnerShipService.unregisterEventNotification(id);
        return super.delete(id);
    }
}
