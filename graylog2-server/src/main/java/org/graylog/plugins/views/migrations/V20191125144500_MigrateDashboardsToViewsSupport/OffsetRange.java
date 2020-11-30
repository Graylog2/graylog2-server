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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class OffsetRange extends TimeRange {
    static final String OFFSET = "offset";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    public abstract Optional<String> id();

    @JsonProperty
    public abstract String offset();

    static OffsetRange ofSearchTypeId(String searchTypeId) {
        return new AutoValue_OffsetRange(OFFSET, "search_type", Optional.of(searchTypeId), "1i");
    }
}
