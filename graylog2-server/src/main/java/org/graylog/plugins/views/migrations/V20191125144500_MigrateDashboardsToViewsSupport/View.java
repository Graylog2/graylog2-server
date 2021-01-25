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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@WithBeanGetter
abstract class View {
    enum Type {
        SEARCH,
        DASHBOARD
    }

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_OWNER = "owner";

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    abstract String id();

    @JsonProperty(FIELD_TYPE)
    abstract Type type();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    abstract String title();

    @JsonProperty(FIELD_SUMMARY)
    abstract String summary();

    @JsonProperty(FIELD_DESCRIPTION)
    abstract String description();

    @JsonProperty(FIELD_SEARCH_ID)
    abstract String searchId();

    @JsonProperty(FIELD_PROPERTIES)
    Set<String> properties() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_REQUIRES)
    Map<String, Object> requires() {
        return Collections.emptyMap();
    }

    @JsonProperty(FIELD_STATE)
    abstract Map<String, ViewState> state();

    @JsonProperty(FIELD_OWNER)
    abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    abstract DateTime createdAt();

    static View create(String id,
                       Type type,
                       String title,
                       String summary,
                       String description,
                       String searchId,
                       Map<String, ViewState> state,
                       Optional<String> owner,
                       DateTime createdAt) {
        return new AutoValue_View(id, type, title, summary, description, searchId, state, owner, createdAt);
    }
}
