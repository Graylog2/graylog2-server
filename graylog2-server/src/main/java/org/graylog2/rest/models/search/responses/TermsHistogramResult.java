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
package org.graylog2.rest.models.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class TermsHistogramResult {
    @JsonProperty("time")
    public abstract long time();

    @JsonProperty("interval")
    public abstract String interval();

    @JsonProperty("size")
    public abstract long size();

    @JsonProperty("buckets")
    public abstract Map<Long, TermsResult> buckets();

    @JsonProperty("terms")
    public abstract Set<String> terms();

    @JsonProperty("built_query")
    public abstract String builtQuery();

    @JsonProperty("queried_timerange")
    public abstract TimeRange queriedTimerange();

    public static TermsHistogramResult create(long time, String interval, long size, Map<Long, TermsResult> buckets, Set<String> terms, String builtQuery, TimeRange queriedTimerange) {
        return new AutoValue_TermsHistogramResult(time, interval, size, buckets, terms, builtQuery, queriedTimerange);
    }
}
