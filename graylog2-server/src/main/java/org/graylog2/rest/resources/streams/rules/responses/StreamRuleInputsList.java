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
package org.graylog2.rest.resources.streams.rules.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.streams.input.StreamRuleInput;

import java.util.Set;

@AutoValue
@WithBeanGetter
public abstract class StreamRuleInputsList {

    @JsonProperty("inputs")
    public abstract Set<StreamRuleInput> inputs();

    public static StreamRuleInputsList create(Set<StreamRuleInput> inputs) {
        return new AutoValue_StreamRuleInputsList(inputs);
    }
}
