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
package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class AWSRegion {

    private static final String VALUE = "value";
    private static final String LABEL = "label";

    // eu-west-2
    @JsonProperty(VALUE)
    public abstract String regionId();

    // The combination of both the name and description for display in the UI:
    // EU (London): eu-west-2
    @JsonProperty(LABEL)
    public abstract String displayValue();

    public static AWSRegion create(@JsonProperty(VALUE) String value,
                                   @JsonProperty(LABEL) String label) {
        return new AutoValue_AWSRegion(value, label);
    }
}