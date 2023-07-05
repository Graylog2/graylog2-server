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
package org.graylog2.contentStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentStream.db.DBContentStreamUserSettingsService;
import org.graylog2.contentStream.rest.ContentStreamService;
import org.graylog2.contentStream.rest.ContentStreamUserSettings;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ContentStreamServiceWithDbTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    EventBus eventBus;

    @Mock
    UserService userService;

    @Mock
    User user1, user2;

    ContentStreamService contentStreamService;

    @Before
    public void setUp() {
        MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        contentStreamService = new ContentStreamService(
                new DBContentStreamUserSettingsService(mongodb.mongoConnection(), mongoJackObjectMapperProvider),
                eventBus
        );
        when(user1.getId()).thenReturn("id1");
        when(user2.getId()).thenReturn("id2");
    }

    @Test
    public void test_no_content_stream_user_settings_present() {
        ContentStreamUserSettings contentStreamUserSettings = contentStreamService.getUserSettings(user1);

        assertThat(contentStreamUserSettings.contentStreamEnabled()).isTrue();
        assertThat(contentStreamUserSettings.topics()).isEmpty();
    }


    @Test
    public void test_save_content_stream_user_settings() {
        saveUserSettings(user2, false, ImmutableList.of());
        ContentStreamUserSettings contentStreamUserSettings = contentStreamService.getUserSettings(user2);

        assertThat(contentStreamUserSettings.contentStreamEnabled()).isFalse();
        assertThat(contentStreamUserSettings.topics()).isEmpty();

        saveUserSettings(user2, false, ImmutableList.of("1", "2"));
        contentStreamUserSettings = contentStreamService.getUserSettings(user2);
        assertThat(contentStreamUserSettings.topics()).containsExactly("1", "2");
    }

    private void saveUserSettings(User user, boolean isEnabled, List<String> topicList) {
        ContentStreamUserSettings userSettings = ContentStreamUserSettings.builder()
                .contentStreamEnabled(isEnabled)
                .topics(topicList)
                .build();
        contentStreamService.saveUserSettings(user, userSettings);
    }

}

