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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.SearchType;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
public abstract class NonImplementedWidget implements ViewWidget {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_ID)
    public abstract String id();
    @JsonProperty(FIELD_TYPE)
    public abstract String type();
    @JsonProperty(FIELD_CONFIG)
    public abstract Map<String, Object> config();

    @Override
    public Set<SearchType> toSearchTypes(RandomUUIDProvider randomUUIDProvider) {
        return Collections.emptySet();
    }

    @JsonCreator
    public static NonImplementedWidget create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_TYPE) String type,
            @JsonProperty(FIELD_CONFIG) Map<String, Object> config
    ) {
        return new AutoValue_NonImplementedWidget(id, type, config);
    }
}
