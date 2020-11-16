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
package org.graylog.plugins.views.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.engine.BackendQuery;

@AutoValue
@JsonTypeName(ElasticsearchQueryString.NAME)
@JsonDeserialize(builder = AutoValue_ElasticsearchQueryString.Builder.class)
public abstract class ElasticsearchQueryString implements BackendQuery {

    public static final String NAME = "elasticsearch";

    public static ElasticsearchQueryString empty() {
        return ElasticsearchQueryString.builder().queryString("").build();
    }

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String queryString();

    @JsonIgnore
    public boolean isEmpty() {
        String trimmed = queryString().trim();
        return trimmed.equals("") || trimmed.equals("*");
    }

    public static Builder builder() {
        return new AutoValue_ElasticsearchQueryString.Builder().type(NAME);
    }

    abstract Builder toBuilder();

    public ElasticsearchQueryString concatenate(ElasticsearchQueryString other) {
        final String thisQueryString = Strings.nullToEmpty(this.queryString()).trim();
        final String otherQueryString = Strings.nullToEmpty(other.queryString()).trim();

        final StringBuilder finalStringBuilder = new StringBuilder(thisQueryString);
        if (!thisQueryString.isEmpty() && !otherQueryString.isEmpty()) {
            finalStringBuilder.append(" AND ");
        }
        if (!otherQueryString.isEmpty()) {
            finalStringBuilder.append(otherQueryString);
        }

        return toBuilder()
                .queryString(finalStringBuilder.toString())
                .build();
    }

    @Override
    public String toString() {
        return type() + ": " + queryString();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder queryString(String queryString);

        public abstract ElasticsearchQueryString build();
    }
}
