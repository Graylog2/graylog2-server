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
package org.graylog.events.notifications;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.graylog.events.event.EventDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class DBNotificationGracePeriodService {
    private static final String NOTIFICATION_STATUS_COLLECTION_NAME = "event_notification_status";

    private final JobSchedulerClock clock;
    private final MongoCollection<EventNotificationStatus> collection;
    private final MongoUtils<EventNotificationStatus> mongoUtils;

    @Inject
    public DBNotificationGracePeriodService(MongoCollections mongoCollections,
                                            JobSchedulerClock clock) {
        collection = mongoCollections.collection(NOTIFICATION_STATUS_COLLECTION_NAME, EventNotificationStatus.class);
        mongoUtils = mongoCollections.utils(collection);
        this.clock = clock;

    }

    public boolean inGracePeriod(EventDto event, String notificationId, long grace) throws NotFoundException {
        EventNotificationStatus status = getNotificationStatus(notificationId, event.eventDefinitionId(), event.key()).orElseThrow(NotFoundException::new);
        Optional<DateTime> lastNotification = status.notifiedAt();

        return lastNotification.map(dateTime -> dateTime.isAfter(event.eventTimestamp().minus(grace))).orElse(false);
    }

    public List<EventNotificationStatus> getAllStatuses() {
        return collection.find().into(new ArrayList<>());
    }

    public int deleteStatus(String statusId) {
        return mongoUtils.deleteById(statusId) ? 1 : 0;
    }

    private Optional<EventNotificationStatus> getNotificationStatus(String notificationId, String definitionId, String key) {
        if (notificationId == null || definitionId == null || key == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(collection.find(and(
                eq(EventNotificationStatus.FIELD_NOTIFICATION_ID, notificationId),
                eq(EventNotificationStatus.FIELD_EVENT_DEFINITION_ID, definitionId),
                eq(EventNotificationStatus.FIELD_EVENT_KEY, key))).first());
    }

    public void updateTriggerStatus(String notificationId, EventDto eventDto, long grace) {
        updateStatus(eventDto, notificationId, "triggered", grace);
    }

    public void updateNotifiedStatus(String notificationId, EventDto eventDto, long grace) {
        updateStatus(eventDto, notificationId, "notified", grace);
    }

    private void updateStatus(EventDto eventDto, String notificationId, String type, long grace) {
        EventNotificationStatus.Builder statusBuilder;
        Optional<EventNotificationStatus> optionalStatus = getNotificationStatus(notificationId, eventDto.eventDefinitionId(), eventDto.key());
        if (optionalStatus.isPresent()) {
            statusBuilder = optionalStatus.get().toBuilder();
        } else {
            statusBuilder = EventNotificationStatus.Builder.create();
        }

        switch (type) {
            case "triggered":
                statusBuilder.triggeredAt(Optional.of(clock.nowUTC()));
                break;
            case "notified":
                statusBuilder.notifiedAt(Optional.of(eventDto.eventTimestamp()));
                break;
        }

        EventNotificationStatus status = statusBuilder
                .notificationId(notificationId)
                .eventKey(eventDto.key())
                .gracePeriodMs(grace)
                .eventDefinitionId(eventDto.eventDefinitionId())
                .build();

        mongoUtils.save(status);
    }
}
