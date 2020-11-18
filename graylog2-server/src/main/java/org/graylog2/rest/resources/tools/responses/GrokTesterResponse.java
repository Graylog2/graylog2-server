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
package org.graylog2.rest.resources.tools.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class GrokTesterResponse {
    @JsonProperty("matched")
    public abstract boolean matched();

    @JsonProperty("matches")
    @Nullable
    public abstract List<Match> matches();

    @JsonProperty("pattern")
    public abstract String pattern();

    @JsonProperty("string")
    public abstract String string();

    @JsonProperty("error_message")
    @Nullable
    public abstract String errorMessage();

    @JsonCreator
    public static GrokTesterResponse create(@JsonProperty("matched") boolean matched,
                                            @JsonProperty("matches") @Nullable List<Match> matches,
                                            @JsonProperty("pattern") String pattern,
                                            @JsonProperty("string") String string,
                                            @JsonProperty("error_message") @Nullable String errorMessage) {
        return new AutoValue_GrokTesterResponse(matched, matches, pattern, string, errorMessage);
    }

    public static GrokTesterResponse createError(String pattern,
                                                 String string,
                                                 @Nullable String errorMessage) {
        return create(false, Collections.emptyList(), pattern, string, errorMessage);
    }

    public static GrokTesterResponse createSuccess(boolean matched,
                                                   @Nullable List<Match> matches,
                                                   String pattern,
                                                   String string) {
        return create(matched, matches, pattern, string, null);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class Match {
        @JsonProperty
        public abstract String name();

        @JsonProperty
        public abstract String match();

        @JsonCreator
        public static Match create(@JsonProperty("name") String name,
                                   @JsonProperty("match") String match) {
            return new AutoValue_GrokTesterResponse_Match(name, match);
        }
    }
}
