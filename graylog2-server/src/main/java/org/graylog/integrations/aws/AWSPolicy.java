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
package org.graylog.integrations.aws;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
// Define a JSON field order matching AWS examples. This improves readability.
@JsonPropertyOrder({AWSPolicy.VERSION, AWSPolicy.STATEMENT})
public abstract class AWSPolicy {

    public static final String VERSION = "Version";
    public static final String STATEMENT = "Statement";

    @JsonProperty(VERSION)
    public abstract String version();

    @JsonProperty(STATEMENT)
    public abstract List<AWSPolicyStatement> statement();

    public static AWSPolicy create(@JsonProperty(VERSION) String version,
                                   @JsonProperty(STATEMENT) List<AWSPolicyStatement> statement) {
        return new AutoValue_AWSPolicy(version, statement);
    }
}