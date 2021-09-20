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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_KeywordRangeEntity.Builder.class)
public abstract class KeywordRangeEntity extends TimeRangeEntity {
    static final String TYPE = "keyword";
    private static final String FIELD_KEYWORD = "keyword";
    private static final String FIELD_TIMEZONE = "timezone";

    @JsonProperty(FIELD_KEYWORD)
    public abstract ValueReference keyword();

    @JsonProperty(FIELD_TIMEZONE)
    public abstract ValueReference timezone();

    public static KeywordRangeEntity of(KeywordRange keywordRange) {
        final String keyword = keywordRange.keyword();
        final String timezone = keywordRange.timezone();
        return builder()
                .keyword(ValueReference.of(keyword))
                .timezone(ValueReference.of(timezone))
                .build();
    }

    static KeywordRangeEntity.Builder builder() {
        return new AutoValue_KeywordRangeEntity.Builder();
    }

    @Override
    public final TimeRange convert(Map<String, ValueReference> parameters) {
        final String keyword = keyword().asString(parameters);
        final String timezone = timezone().asString(parameters);
        try {
            return KeywordRange.create(keyword, timezone);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("Invalid timerange.", e);
        }
    }

    @AutoValue.Builder
    abstract static class Builder implements TimeRangeBuilder<Builder> {
        @JsonProperty(FIELD_KEYWORD)
        abstract Builder keyword(ValueReference keyword);

        @JsonProperty(FIELD_TIMEZONE)
        abstract Builder timezone(ValueReference timezone);

        abstract KeywordRangeEntity autoBuild();

        KeywordRangeEntity build() {
            type(ModelTypeEntity.of(TYPE));
            return autoBuild();
        }
    }
}
