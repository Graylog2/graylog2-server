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
package org.graylog2.rest.models.system.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.PaginatedList;
import org.graylog2.grok.GrokPattern;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class GrokPatternPageList {
    @Nullable
    @JsonProperty
    public abstract String query();


    @JsonProperty("pagination")
    public abstract PaginatedList.PaginationInfo paginationInfo();

    @JsonProperty
    public abstract long total();

    @Nullable
    @JsonProperty
    public abstract String sort();

    @Nullable
    @JsonProperty
    public abstract String order();

    @JsonProperty
    public abstract Collection<GrokPattern> patterns();


    @JsonCreator
    public static GrokPatternPageList create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("pagination") PaginatedList.PaginationInfo paginationInfo,
            @JsonProperty("total") long total,
            @JsonProperty("sort") @Nullable String sort,
            @JsonProperty("order") @Nullable String order,
            @JsonProperty("patterns") Collection<GrokPattern> patternList) {
        return new AutoValue_GrokPatternPageList(query, paginationInfo, total, sort, order, patternList);
    }
}
