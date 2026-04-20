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
import org.graylog.events.context.EventDefinitionContextService;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog2.database.DbEntity;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Set;

import static org.graylog.events.processor.EventDefinitionDto.FIELD_ALERT;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_DESCRIPTION;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_MATCHED_AT;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_PRIORITY;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_STATE;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_TITLE;
import static org.graylog.events.processor.EventDefinitionDto.FIELD_UPDATED_AT;
import static org.graylog2.database.MongoEntity.FIELD_ID;
import static org.graylog2.shared.security.EntityPermissionsUtils.ID_FIELD;

@DbEntity(readPermission = RestPermissions.EVENT_DEFINITIONS_READ,
          collection = DBEventDefinitionService.COLLECTION_NAME,
          readableFields = {ID_FIELD, FIELD_ID, FIELD_TITLE, FIELD_DESCRIPTION, FIELD_UPDATED_AT, FIELD_MATCHED_AT,
                  FIELD_PRIORITY, FIELD_ALERT, FIELD_STATE
          })
public interface EventDefinition {
    enum State {
        ENABLED,
        DISABLED
    }

    @Nullable
    String id();

    String title();

    String description();

    default String remediationSteps() {
        return null;
    }

    @Nullable
    DateTime updatedAt();

    @Nullable
    DateTime matchedAt();

    int priority();

    boolean alert();

    EventProcessorConfig config();

    ImmutableMap<String, EventFieldSpec> fieldSpec();

    ImmutableList<String> keySpec();

    EventNotificationSettings notificationSettings();

    ImmutableList<EventNotificationHandler.Config> notifications();

    ImmutableList<EventStorageHandler.Config> storage();

    EventDefinitionContextService.SchedulerCtx schedulerCtx();

    default Set<String> requiredPermissions() {
        return config().requiredPermissions();
    }

    default String eventProcedureId() {
        return null;
    }

    @Nullable
    default String eventSummaryTemplate() {
        return null;
    }
}
