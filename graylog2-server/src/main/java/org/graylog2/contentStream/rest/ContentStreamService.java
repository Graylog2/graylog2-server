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
package org.graylog2.contentStream.rest;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.contentStream.db.ContentStreamUserSettingsDto;
import org.graylog2.contentStream.db.DBContentStreamUserSettingsService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.events.UserDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class ContentStreamService {

    private static final Logger LOG = LoggerFactory.getLogger(ContentStreamService.class);
    private final ContentStreamResponseFactory contentStreamResponseFactory;
    private final DBContentStreamUserSettingsService dbContentStreamUserSettingsService;

    @Inject
    public ContentStreamService(
            ContentStreamResponseFactory contentStreamResponseFactory,
            DBContentStreamUserSettingsService dbContentStreamUserSettingsService,
            EventBus eventBus) {
        this.contentStreamResponseFactory = contentStreamResponseFactory;
        this.dbContentStreamUserSettingsService = dbContentStreamUserSettingsService;
        eventBus.register(this);
    }

    public Map<String, Object> getContentStreamResponse(User user) {
        ContentStreamUserSettings contentStreamUserSettings = getContentStreamUserSettings(user);
        return contentStreamResponseFactory.createContentStreamResponse(contentStreamUserSettings);
    }

    public ContentStreamUserSettings getContentStreamUserSettings(User user) {
        Optional<ContentStreamUserSettingsDto> dto = dbContentStreamUserSettingsService.findByUserId(user.getId());
        Boolean enabled = dto.isPresent() ? dto.get().contentStreamEnabled() : true;
        return ContentStreamUserSettings.builder().contentStreamEnabled(enabled).build();
    }

    public void saveUserSettings(User user, ContentStreamUserSettings contentStreamUserSettings) {
        ContentStreamUserSettingsDto.Builder builder = ContentStreamUserSettingsDto.builder()
                .userId(user.getId())
                .contentStreamEnabled(contentStreamUserSettings.contentStreamEnabled());
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
