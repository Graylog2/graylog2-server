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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class SearchDecorationStats {
    private static final String FIELD_ADDED_FIELDS = "added_fields";
    private static final String FIELD_CHANGED_FIELDS = "changed_fields";
    private static final String FIELD_REMOVED_FIELDS = "removed_fields";

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_ADDED_FIELDS)
    public abstract Set<String> addedFields();

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_CHANGED_FIELDS)
    public abstract Set<String> changedFields();

    @SuppressWarnings("unused")
    @JsonProperty(FIELD_REMOVED_FIELDS)
    public abstract Set<String> removedFields();

    @JsonCreator
    public static SearchDecorationStats create(@JsonProperty(FIELD_ADDED_FIELDS) Set<String> addedFields,
                                               @JsonProperty(FIELD_CHANGED_FIELDS) Set<String> changedFields,
                                               @JsonProperty(FIELD_REMOVED_FIELDS) Set<String> removedFields) {
        return new AutoValue_SearchDecorationStats(addedFields, changedFields, removedFields);
    }
}
