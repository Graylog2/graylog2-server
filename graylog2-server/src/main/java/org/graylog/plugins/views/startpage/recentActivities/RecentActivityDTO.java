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
package org.graylog.plugins.views.startpage.recentActivities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = RecentActivityDTO.Builder.class)
@WithBeanGetter
public abstract class RecentActivityDTO {
    public static final String FIELD_ID = "id";
    public static final String FIELD_ACTIVITY_TYPE = "activity_type";
    public static final String FIELD_ITEM_GRN = "item_grn";
    public static final String FIELD_ITEM_ID = "item_id";
    public static final String FIELD_ITEM_TYPE = "item_type";
    public static final String FIELD_ITEM_TITLE = "item_title";
    public static final String FIELD_USER_NAME = "user_name";
    public static final String FIELD_GRANTEE = "grantee";
    public static final String FIELD_TIMESTAMP = "timestamp";

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_ACTIVITY_TYPE)
    public abstract ActivityType activityType();

    @JsonProperty(FIELD_ITEM_GRN)
    public abstract String itemGrn();

    @JsonProperty(FIELD_ITEM_ID)
    public abstract String itemId();

    @Nullable
    @JsonProperty(FIELD_ITEM_TYPE)
    public abstract String itemType();

    @Nullable
    @JsonProperty(FIELD_ITEM_TITLE)
    public abstract String itemTitle();

    @Nullable
    @JsonProperty(FIELD_USER_NAME)
    public abstract String userName();

    @Nullable
    @JsonProperty(FIELD_GRANTEE)
    public abstract String grantee();

    @JsonProperty(FIELD_TIMESTAMP)
    public abstract DateTime timestamp();

    public static RecentActivityDTO.Builder builder() {
        return RecentActivityDTO.Builder.create();
    }

    public abstract RecentActivityDTO.Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract RecentActivityDTO.Builder id(String id);

        @JsonProperty(FIELD_ACTIVITY_TYPE)
        public abstract RecentActivityDTO.Builder activityType(ActivityType activityType);

        @JsonProperty(FIELD_ITEM_GRN)
        public abstract RecentActivityDTO.Builder itemGrn(String itemGrn);

        @JsonProperty(FIELD_ITEM_ID)
        public abstract RecentActivityDTO.Builder itemId(String itemId);

        @JsonProperty(FIELD_ITEM_TYPE)
        public abstract RecentActivityDTO.Builder itemType(String itemType);

        @JsonProperty(FIELD_ITEM_TITLE)
        public abstract RecentActivityDTO.Builder itemTitle(String itemTitle);

        @JsonProperty(FIELD_USER_NAME)
        public abstract RecentActivityDTO.Builder userName(String userName);

        @JsonProperty(FIELD_GRANTEE)
        public abstract RecentActivityDTO.Builder grantee(String grantee);

        @JsonProperty(FIELD_TIMESTAMP)
        public abstract RecentActivityDTO.Builder timestamp(DateTime timestamp);

        @JsonCreator
        public static RecentActivityDTO.Builder create() {
            return new $AutoValue_RecentActivityDTO.Builder().timestamp(new DateTime(DateTimeZone.UTC));
        }

        public abstract RecentActivityDTO build();
    }
}
