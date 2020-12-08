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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
abstract class MessageList implements SearchType {
    static final String NAME = "messages";

    @JsonProperty
    String type() { return NAME;}

    @Nullable
    Object filter() { return null; }

    @JsonProperty
    abstract int limit();

    @JsonProperty
    abstract int offset();

    @Nullable
    abstract List<Sort> sort();

    @JsonCreator
    static Builder builder() {
        return new AutoValue_MessageList.Builder()
                .limit(150)
                .offset(0)
                .streams(Collections.emptySet());
    }

    abstract Builder toBuilder();

    @AutoValue.Builder
    abstract static class Builder {
        @JsonCreator
        static Builder createDefault() {
            return builder()
                    .streams(Collections.emptySet());
        }

        abstract Builder id(String id);

        abstract Builder timerange(@Nullable TimeRange timerange);

        abstract Builder query(@Nullable ElasticsearchQueryString query);
        Builder query(String query) {
            return query(ElasticsearchQueryString.create(query));
        }

        abstract Builder streams(Set<String> streams);

        abstract Builder limit(int limit);

        abstract Builder offset(int offset);

        abstract Builder sort(@Nullable List<Sort> sort);

        abstract Builder name(@Nullable String name);

        abstract MessageList build();
    }
}
