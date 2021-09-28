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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.graylog.plugins.views.search.engine.BackendQuery;

import javax.annotation.Nullable;
import java.util.Objects;

@JsonAutoDetect
public class ElasticsearchQueryString implements BackendQuery {

    public static final String NAME = "elasticsearch";
    private final String queryString;

    @JsonCreator
    public ElasticsearchQueryString(String queryString) {
        this.queryString = queryString;
    }

    @JsonCreator
    public ElasticsearchQueryString(@JsonProperty("type") String type, @JsonProperty("query_string") String query) {
        this.queryString = query;
    }

    public static ElasticsearchQueryString empty() {
        return ElasticsearchQueryString.of("");
    }

    public static ElasticsearchQueryString of(final String query) {
        return new ElasticsearchQueryString(NAME, query);
    }

    @Nullable
    @Override
    public String type() {
        return NAME;
    }

    @JsonProperty
    public String queryString() {
        return this.queryString;
    }

    @JsonIgnore
    public boolean isEmpty() {
        String trimmed = queryString().trim();
        return trimmed.equals("") || trimmed.equals("*");
    }

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
        return new ElasticsearchQueryString(NAME, finalStringBuilder.toString());
    }

    @Override
    public String toString() {
        return type() + ": " + queryString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ElasticsearchQueryString that = (ElasticsearchQueryString) o;
        return Objects.equals(queryString, that.queryString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queryString);
    }
}
