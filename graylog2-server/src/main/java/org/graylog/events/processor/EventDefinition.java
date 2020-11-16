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
import com.google.common.collect.ImmutableMap;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.EventStorageHandler;

import java.util.Set;

public interface EventDefinition {
    String id();

    String title();

    String description();

    int priority();

    boolean alert();

    EventProcessorConfig config();

    ImmutableMap<String, EventFieldSpec> fieldSpec();

    ImmutableList<String> keySpec();

    EventNotificationSettings notificationSettings();

    ImmutableList<EventNotificationHandler.Config> notifications();

    ImmutableList<EventStorageHandler.Config> storage();

    default Set<String> requiredPermissions() {
        return config().requiredPermissions();
    }
}
