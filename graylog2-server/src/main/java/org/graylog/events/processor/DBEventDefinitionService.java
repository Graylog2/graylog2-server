/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.processor;

import com.google.common.collect.ImmutableList;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class DBEventDefinitionService extends PaginatedDbService<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(DBEventDefinitionService.class);

    private static final String COLLECTION_NAME = "event_definitions";

    private final DBEventProcessorStateService stateService;
    private final EntityOwnershipService entityOwnerShipService;

    @Inject
    public DBEventDefinitionService(MongoConnection mongoConnection,
                                    MongoJackObjectMapperProvider mapper,
                                    DBEventProcessorStateService stateService,
                                    EntityOwnershipService entityOwnerShipService) {
        super(mongoConnection, mapper, EventDefinitionDto.class, COLLECTION_NAME);
        this.stateService = stateService;
        this.entityOwnerShipService = entityOwnerShipService;
    }

    public PaginatedList<EventDefinitionDto> searchPaginated(SearchQuery query, Predicate<EventDefinitionDto> filter,
                                                             String sortByField, int page, int perPage) {
        return findPaginatedWithQueryFilterAndSort(query.toDBQuery(), filter,
                getSortBuilder("asc", sortByField), page, perPage);
    }

    public EventDefinitionDto saveWithOwnership(EventDefinitionDto eventDefinitionDto, User user) {
        final EventDefinitionDto dto = super.save(eventDefinitionDto);
        entityOwnerShipService.registerNewEventDefinition(dto.id(), user);
        return dto;
    }

    @Override
    public int delete(String id) {
        try {
            stateService.deleteByEventDefinitionId(id);
        } catch (Exception e) {
            LOG.error("Couldn't delete event processor state for <{}>", id, e);
        }
        entityOwnerShipService.unregisterEventDefinition(id);
        return super.delete(id);
    }

    /**
     * Returns the list of event definitions that is using the given notification ID.
     *
     * @param notificationId the notification ID
     * @return the event definitions with the given notification ID
     */
    public List<EventDefinitionDto> getByNotificationId(String notificationId) {
        final String field = String.format(Locale.US, "%s.%s",
            EventDefinitionDto.FIELD_NOTIFICATIONS,
            EventNotificationConfig.FIELD_NOTIFICATION_ID);
        return ImmutableList.copyOf((db.find(DBQuery.is(field, notificationId)).iterator()));
    }
}
