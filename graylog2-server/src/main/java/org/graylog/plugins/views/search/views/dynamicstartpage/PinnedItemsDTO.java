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
package org.graylog.plugins.views.search.views.dynamicstartpage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = PinnedItemsDTO.Builder.class)
@WithBeanGetter
public abstract class PinnedItemsDTO {
    public static final String FIELD_ID = "id";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_ITEMS = "items";

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_USER_ID)
    public abstract String userId();

    @JsonProperty(FIELD_ITEMS)
    public abstract List<String> items();

    public static PinnedItemsDTO.Builder builder() {
        return PinnedItemsDTO.Builder.create();
    }

    public abstract PinnedItemsDTO.Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract PinnedItemsDTO.Builder id(String id);

        @JsonProperty(FIELD_USER_ID)
        public abstract PinnedItemsDTO.Builder userId(String userId);

        @JsonProperty(FIELD_ITEMS)
        public abstract PinnedItemsDTO.Builder items(List<String> items);

        @JsonCreator
        public static PinnedItemsDTO.Builder create() {
            return new AutoValue_PinnedItemsDTO.Builder()
                    .items(new ArrayList<>());
        }

        public abstract PinnedItemsDTO build();
    }
}
