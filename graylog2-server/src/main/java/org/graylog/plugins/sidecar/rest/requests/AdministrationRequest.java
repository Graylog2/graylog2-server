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
package org.graylog.plugins.sidecar.rest.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@AutoValue
@JsonAutoDetect
public abstract class AdministrationRequest {
    private final static int DEFAULT_PAGE = 1;
    private final static int DEFAULT_PER_PAGE = 50;

    @JsonProperty
    public abstract int page();

    @JsonProperty
    public abstract int perPage();

    @JsonProperty
    public abstract String query();

    @JsonProperty
    public abstract Map<String, String> filters();

    @JsonCreator
    public static AdministrationRequest create(@JsonProperty("page") int page,
                                               @JsonProperty("per_page") int perPage,
                                               @JsonProperty("query") @Nullable String query,
                                               @JsonProperty("filters") @Nullable Map<String, String> filters) {
        final int effectivePage = page == 0 ? DEFAULT_PAGE : page;
        final int effectivePerPage = perPage == 0 ? DEFAULT_PER_PAGE : perPage;
        return new AutoValue_AdministrationRequest(
                effectivePage,
                effectivePerPage,
                firstNonNull(query, ""),
                firstNonNull(filters, new HashMap<>()));
    }
}
