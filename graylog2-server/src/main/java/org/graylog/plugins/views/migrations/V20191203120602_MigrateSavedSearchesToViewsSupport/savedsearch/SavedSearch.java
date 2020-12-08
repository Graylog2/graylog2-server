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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class SavedSearch {
    public abstract String id();
    public abstract String title();
    public abstract Query query();
    public abstract DateTime createdAt();
    public abstract String creatorUserId();

    @JsonCreator
    static SavedSearch create(
            @JsonProperty("_id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("query") Query query,
            @JsonProperty("created_at") DateTime createdAt,
            @JsonProperty("creator_user_id") String creatorUserId
    ) {
        return new AutoValue_SavedSearch(id, title, query, createdAt, creatorUserId);
    }
}
