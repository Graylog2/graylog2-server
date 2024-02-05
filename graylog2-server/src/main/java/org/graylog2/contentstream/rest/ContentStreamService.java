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
import org.graylog2.contentstream.ContentStreamFeedTags;
import org.graylog2.contentstream.db.ContentStreamUserSettings;
import org.graylog2.contentstream.db.DBContentStreamUserSettingsService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.users.events.UserDeletedEvent;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContentStreamService {
    private final DBContentStreamUserSettingsService dbContentStreamUserSettingsService;
    private final ContentStreamFeedTags contentStreamFeedTags;

    @Inject
    public ContentStreamService(
            DBContentStreamUserSettingsService dbContentStreamUserSettingsService,
            ContentStreamFeedTags contentStreamFeedTags,
            EventBus eventBus) {
        this.dbContentStreamUserSettingsService = dbContentStreamUserSettingsService;
        this.contentStreamFeedTags = contentStreamFeedTags;
        eventBus.register(this);
    }

    /**
     * Determine set of valid feed tags based on license
     *
     * @return list of feed tags
     */
    public List<String> getTags() {
        return contentStreamFeedTags.getTags();
    }

    public ContentStreamSettings getUserSettings(User user) {
        Optional<ContentStreamUserSettings> dto = dbContentStreamUserSettingsService.findByUserId(user.getId());
        if (dto.isPresent()) {
            return ContentStreamSettings.builder()
                    .contentStreamEnabled(dto.get().contentStreamEnabled())
                    .releasesEnabled(dto.get().releasesEnabled())
                    .topics(dto.get().topics())
                    .build();
        }
        return ContentStreamSettings.builder()
                .contentStreamEnabled(true)
                .releasesEnabled(true)
                .topics(new ArrayList<>())
                .build();
    }

    public void saveUserSettings(User user, ContentStreamSettings settings) {
        ContentStreamUserSettings.Builder builder = ContentStreamUserSettings.builder()
                .userId(user.getId())
                .contentStreamEnabled(settings.contentStreamEnabled())
                .releasesEnabled(settings.releasesEnabled())
                .topics(settings.topics());
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
