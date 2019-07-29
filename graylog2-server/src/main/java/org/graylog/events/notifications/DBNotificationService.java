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
