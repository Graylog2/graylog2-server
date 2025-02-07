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
package org.graylog.plugins.views.search.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.SearchJobIdentifier;
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = SearchJobState.Builder.class)
public abstract class SearchJobState implements MongoEntity {

    public static final String CREATED_AT_FIELD = "created_at";
    public static final String UPDATED_AT_FIELD = "updated_at";
    public static final String STATUS_FIELD = "status";
    public static final String TYPE_FIELD = "type";
    public static final String RESULT_FIELD = "result";

    @JsonUnwrapped
    public abstract SearchJobIdentifier identifier();

    @JsonProperty(STATUS_FIELD)
    public abstract SearchJobStatus status();

    @JsonProperty(TYPE_FIELD)
    public abstract SearchJobType type();

    @JsonProperty("error_message")
    public abstract String errorMessage();

    @JsonProperty("progress")
    public abstract int progress();

    @JsonProperty(RESULT_FIELD)
    @Nullable
    public abstract SearchType.Result result();

    @JsonProperty(CREATED_AT_FIELD)
    public abstract DateTime createdAt();

    @JsonProperty(UPDATED_AT_FIELD)
    public abstract DateTime updatedAt();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonUnwrapped
        public abstract Builder identifier(final SearchJobIdentifier identifier);

        @JsonProperty(STATUS_FIELD)
        public abstract Builder status(final SearchJobStatus status);

        @JsonProperty(TYPE_FIELD)
        public abstract Builder type(final SearchJobType type);

        @JsonProperty("error_message")
        public abstract Builder errorMessage(final String errorMessage);

        @JsonProperty("progress")
        public abstract Builder progress(final int progress);

        @JsonProperty(RESULT_FIELD)
        public abstract Builder result(final SearchType.Result result);

        @JsonProperty(CREATED_AT_FIELD)
        public abstract Builder createdAt(final DateTime createdAt);

        @JsonProperty(UPDATED_AT_FIELD)
        public abstract Builder updatedAt(final DateTime updatedAt);

        public abstract SearchJobState build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SearchJobState.Builder()
                    .progress(0)
                    .type(SearchJobType.DATA_LAKE)
                    .errorMessage("");
        }
    }

    public static SearchJobState createNewJob(final SearchJobIdentifier searchJobIdentifier) {
        return SearchJobState.builder()
                .identifier(searchJobIdentifier)
                .result(null)
                .status(SearchJobStatus.RUNNING)
                .progress(0)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
    }

    public static SearchJobState createDoneJobFrom(final SearchJobState existingSearchJob,
                                                   final SearchType.Result result) {
        return existingSearchJob.toBuilder()
                .result(result)
                .status(SearchJobStatus.DONE)
                .progress(100)
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
    }

    @Override
    @ObjectId
    @Id
    @JsonIgnore
    public String id() {
        return identifier().id();
    }
}
