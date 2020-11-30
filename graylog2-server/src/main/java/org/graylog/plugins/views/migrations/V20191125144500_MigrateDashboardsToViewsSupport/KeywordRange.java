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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class KeywordRange extends TimeRange {
    static final String KEYWORD = "keyword";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    abstract String keyword();

    @JsonCreator
    static KeywordRange create(@JsonProperty("type") String type, @JsonProperty("keyword") String keyword) {
        return builder().type(type).keyword(keyword).build();
    }

    public static KeywordRange create(String keyword) {
        return create(KEYWORD, keyword);
    }

    private static Builder builder() {
        return new AutoValue_KeywordRange.Builder();
    }

    String getKeyword() {
        return keyword();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder type(String type);

        abstract Builder keyword(String keyword);

        abstract String keyword();

        abstract KeywordRange build();
    }
}

