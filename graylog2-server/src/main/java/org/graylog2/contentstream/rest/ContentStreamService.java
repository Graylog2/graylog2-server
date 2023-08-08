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
package org.graylog2.contentstream.rest;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.contentstream.db.ContentStreamUserSettings;
import org.graylog2.contentstream.db.DBContentStreamUserSettingsService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.events.UserDeletedEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContentStreamService {
    private final DBContentStreamUserSettingsService dbContentStreamUserSettingsService;

    @Inject
    public ContentStreamService(
            DBContentStreamUserSettingsService dbContentStreamUserSettingsService,
            EventBus eventBus) {
        this.dbContentStreamUserSettingsService = dbContentStreamUserSettingsService;
        eventBus.register(this);
    }

    public ContentStreamSettings getUserSettings(User user) {
        Optional<ContentStreamUserSettings> dto = dbContentStreamUserSettingsService.findByUserId(user.getId());
        Boolean isEnabled = dto.isPresent() ? dto.get().contentStreamEnabled() : true;
        List<String> topicList = dto.isPresent() && dto.get().topics() != null ? dto.get().topics() : new ArrayList<>();
        return ContentStreamSettings.builder()
                .contentStreamEnabled(isEnabled)
                .topics(topicList)
                .build();
    }

    public void saveUserSettings(User user, ContentStreamSettings contentStreamSettings) {
        ContentStreamUserSettings.Builder builder = ContentStreamUserSettings.builder()
                .userId(user.getId())
                .contentStreamEnabled(contentStreamSettings.contentStreamEnabled())
                .topics(contentStreamSettings.topics());
        dbContentStreamUserSettingsService.findByUserId(user.getId()).ifPresent(dto -> builder.id(dto.id()));
        dbContentStreamUserSettingsService.save(builder.build());
    }

    public void deleteUserSettingsByUser(User user) {
        deleteUserSettingsByUserId(user.getId());
    }

    @Subscribe
    private void handleUserDeletedEvent(UserDeletedEvent event) {
        deleteUserSettingsByUserId(event.userId());
    }

    private void deleteUserSettingsByUserId(String userId) {
        dbContentStreamUserSettingsService.delete(userId);
    }
}
