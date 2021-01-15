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
package org.graylog2.indexer.searches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class ScrollCommand {
    public static final int NO_BATCHSIZE = -1;
    public static final int NO_LIMIT = -1;

    public abstract String query();
    public abstract Set<String> indices();
    public abstract Optional<Set<String>> streams();
    public abstract Optional<Sorting> sorting();
    public abstract Optional<String> filter();
    public abstract Optional<TimeRange> range();
    public abstract OptionalInt limit();
    public abstract OptionalInt offset();
    public abstract List<String> fields();
    public abstract OptionalLong batchSize();
    public abstract boolean highlight();

    public static Builder builder() {
        return new AutoValue_ScrollCommand.Builder()
                .query("")
                .fields(Collections.emptyList())
                .highlight(false);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder query(String query);
        public abstract Builder indices(Set<String> indices);
        public abstract Builder streams(Set<String> streams);
        public abstract Builder sorting(Sorting sorting);
        public abstract Builder filter(@Nullable String filter);
        public abstract Builder range(TimeRange range);
        public abstract Builder limit(int limit);
        public abstract Builder offset(int offset);
        public abstract Builder fields(List<String> fields);
        public abstract Builder batchSize(int batchSize);
        public abstract Builder highlight(boolean highlight);

        public abstract ScrollCommand build();
    }
}
