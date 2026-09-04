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
package org.graylog2.rest.models.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class RegexTesterResponse {
    @JsonProperty("matched")
    public abstract boolean matched();

    @JsonProperty("match")
    @Nullable
    public abstract Match match();

    @JsonProperty("regex")
    public abstract String regex();

    @JsonProperty("string")
    public abstract String string();

    @JsonCreator
    public static RegexTesterResponse create(@JsonProperty("matched") boolean matched,
                                             @JsonProperty("match") @Nullable Match match,
                                             @JsonProperty("regex") String regex,
                                             @JsonProperty("string") String string) {
        return new AutoValue_RegexTesterResponse(matched, match, regex, string);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class Match {
        @JsonProperty("match")
        @Nullable
        public abstract String match();

        @JsonProperty("start")
        public abstract int start();

        @JsonProperty("end")
        public abstract int end();

        @JsonCreator
        public static Match create(@JsonProperty("match") @Nullable String match,
                                   @JsonProperty("start") int start,
                                   @JsonProperty("end") int end) {
            return new AutoValue_RegexTesterResponse_Match(match, start, end);
        }
    }
}
