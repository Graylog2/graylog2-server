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
package org.graylog.plugins.views.search.querystrings;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public record QueryStringForUser(@ObjectId
                                 @Id
                                 @Nullable
                                 @JsonProperty(FIELD_ID)
                                 String id,
                                 @JsonProperty(FIELD_USER_ID)
                                 String userId,
                                 @Nullable
                                 @JsonProperty(FIELD_ITEMS)
                                 List<QueryString> items) {
    public static final String FIELD_ID = "id";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_ITEMS = "items";

    public QueryStringForUser {
        if (items == null) {
            items = new ArrayList<>();
        }
    }

    public QueryStringForUser(String userId, @Nullable List<QueryString> items) {
        this(null, userId, items);
    }
}
