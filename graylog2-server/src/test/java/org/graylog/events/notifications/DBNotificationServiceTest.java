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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MongoDBExtension.class)
public class DBNotificationServiceTest {

    private DBNotificationService dbService;

    @BeforeEach
    public void setUp(MongoDBTestService dbTestService) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(HTTPEventNotificationConfig.class);
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        this.dbService = new DBNotificationService(
                new MongoCollections(mapperProvider, dbTestService.mongoConnection()),
                mock(EntityRegistrar.class));
    }

    @Test
    public void getByIds_returnsOnlyMatchingDocuments() {
        final NotificationDto first = dbService.save(notificationWithTitle("First"));
        final NotificationDto second = dbService.save(notificationWithTitle("Second"));
        dbService.save(notificationWithTitle("Third"));

        final List<NotificationDto> results = dbService.getByIds(List.of(first.id(), second.id()));

        assertThat(results)
                .extracting(NotificationDto::title)
                .containsExactlyInAnyOrder("First", "Second");
    }

    @Test
    public void getByIds_skipsInvalidObjectIds() {
        final NotificationDto saved = dbService.save(notificationWithTitle("First"));

        final List<NotificationDto> results = dbService.getByIds(List.of(saved.id(), "not-an-objectid"));

        assertThat(results)
                .extracting(NotificationDto::title)
                .containsExactly("First");
    }

    @Test
    public void getByIds_returnsEmptyWhenNoIdsProvided() {
        assertThat(dbService.getByIds(List.of())).isEmpty();
    }

    @Test
    public void getByIds_returnsEmptyWhenNoIdsAreValidObjectIds() {
        assertThat(dbService.getByIds(List.of("not-an-objectid", "also-not-valid"))).isEmpty();
    }

    private NotificationDto notificationWithTitle(String title) {
        return NotificationDto.builder()
                .title(title)
                .description("")
                .config(HTTPEventNotificationConfig.Builder.create()
                        .url("http://localhost")
                        .build())
                .build();
    }
}
