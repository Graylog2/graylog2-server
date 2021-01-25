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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class SubstringTesterResponse {
    @JsonProperty
    public abstract boolean successful();

    @JsonProperty
    @Nullable
    public abstract String cut();

    @JsonProperty("begin_index")
    public abstract int beginIndex();

    @JsonProperty("end_index")
    public abstract int endIndex();

    @JsonCreator
    public static SubstringTesterResponse create(@JsonProperty("successful")boolean successful,
                                                 @JsonProperty("cut") @Nullable String cut,
                                                 @JsonProperty("begin_index") int beginIndex,
                                                 @JsonProperty("end_index")int endIndex) {
        return new AutoValue_SubstringTesterResponse(successful, cut, beginIndex, endIndex);
    }
}
