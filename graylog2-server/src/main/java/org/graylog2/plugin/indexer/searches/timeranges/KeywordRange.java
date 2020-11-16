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
package org.graylog2.plugin.indexer.searches.timeranges;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.utilities.date.NaturalDateParser;
import org.joda.time.DateTime;

@AutoValue
@JsonTypeName(KeywordRange.KEYWORD)
public abstract class KeywordRange extends TimeRange {

    private static final NaturalDateParser DATE_PARSER = new NaturalDateParser();

    public static final String KEYWORD = "keyword";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty
    public abstract String keyword();

    private static NaturalDateParser.Result parseResult(String keyword) throws InvalidRangeParametersException {
        try {
            return DATE_PARSER.parse(keyword);
        } catch (NaturalDateParser.DateNotParsableException e) {
            throw new InvalidRangeParametersException("Could not parse from natural date: " + keyword);
        }
    }

    @JsonCreator
    public static KeywordRange create(@JsonProperty("type") String type, @JsonProperty("keyword") String keyword) throws InvalidRangeParametersException {
        return builder().type(type).keyword(keyword).build();
    }

    public static KeywordRange create(String keyword) throws InvalidRangeParametersException {
        return create(KEYWORD, keyword);
    }

    private static Builder builder() {
        return new AutoValue_KeywordRange.Builder();
    }

    public String getKeyword() {
        return keyword();
    }

    @JsonIgnore
    @Override
    public DateTime getFrom() {
        try {
            return parseResult(keyword()).getFrom();
        } catch (InvalidRangeParametersException e) {
            return null;
        }
    }

    @JsonIgnore
    @Override
    public DateTime getTo() {
        try {
            return parseResult(keyword()).getTo();
        } catch (InvalidRangeParametersException e) {
            return null;
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);

        public abstract Builder keyword(String keyword);

        abstract String keyword();

        abstract KeywordRange autoBuild();

        public KeywordRange build() throws InvalidRangeParametersException {
            parseResult(keyword());
            return autoBuild();
        }
    }
}

