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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.firstNonNull;

@AutoValue
@JsonAutoDetect
abstract class Dashboard {
    private static final String FIELD_ID = "_id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CONTENT_PACK = "content_pack";
    private static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String EMBEDDED_WIDGETS = "widgets";
    private static final String EMBEDDED_POSITIONS = "positions";

    @JsonProperty(FIELD_ID)
    abstract String id();

    @JsonProperty(FIELD_TITLE)
    abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    abstract String description();

    @JsonProperty(FIELD_CONTENT_PACK)
    abstract Optional<String> contentPack();

    @JsonProperty(FIELD_CREATOR_USER_ID)
    abstract String creatorUserId();

    @JsonProperty(FIELD_CREATED_AT)
    abstract DateTime createdAt();

    @JsonProperty(EMBEDDED_POSITIONS)
    abstract Map<String, WidgetPosition> widgetPositions();

    @JsonProperty(EMBEDDED_WIDGETS)
    abstract List<Widget> widgets();

    @JsonCreator
    static Dashboard create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_TITLE) String title,
            @JsonProperty(FIELD_DESCRIPTION) String description,
            @JsonProperty(FIELD_CONTENT_PACK) @Nullable String contentPack,
            @JsonProperty(FIELD_CREATOR_USER_ID) String creatorUserId,
            @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
            @JsonProperty(EMBEDDED_POSITIONS) @Nullable Map<String, WidgetPosition> widgetPositions,
            @JsonProperty(EMBEDDED_WIDGETS) @Nullable List<Widget> widgets
    ) {
        return new AutoValue_Dashboard(id,
                title,
                description,
                Optional.ofNullable(contentPack),
                creatorUserId,
                createdAt,
                firstNonNull(widgetPositions, Collections.emptyMap()),
                firstNonNull(widgets, Collections.emptyList()));
    }
}
