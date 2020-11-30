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
package org.graylog.plugins.views.search.timeranges;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.GlobalOverride;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class DerivedTimeRange {
    @JsonValue
    abstract TimeRange value();

    public TimeRange effectiveTimeRange(Query query, SearchType searchType) {
        if (value() instanceof DerivableTimeRange) {
            return ((DerivableTimeRange)value()).deriveTimeRange(query, searchType);
        }

        return query.globalOverride().flatMap(GlobalOverride::timerange).orElse(value());
    }

    @JsonCreator
    public static DerivedTimeRange of(TimeRange timeRange) {
        return new AutoValue_DerivedTimeRange(timeRange);
    }
}
