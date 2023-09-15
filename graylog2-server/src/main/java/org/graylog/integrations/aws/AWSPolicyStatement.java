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
@JsonPropertyOrder({AWSPolicyStatement.SID, AWSPolicyStatement.EFFECT, AWSPolicyStatement.ACTION, AWSPolicyStatement.RESOURCE})
public abstract class AWSPolicyStatement {

    static final String SID = "Sid";
    static final String EFFECT = "Effect";
    static final String ACTION = "Action";
    static final String RESOURCE = "Resource";

    @JsonProperty(SID)
    public abstract String sid();

    @JsonProperty(EFFECT)
    public abstract String effect();

    @JsonProperty(ACTION)
    public abstract List<String> action();

    @JsonProperty(RESOURCE)
    public abstract String resource();

    public static AWSPolicyStatement create(@JsonProperty(SID) String sid,
                                            @JsonProperty(EFFECT) String effect,
                                            @JsonProperty(ACTION) List<String> action,
                                            @JsonProperty(RESOURCE) String resource) {
        return new AutoValue_AWSPolicyStatement(sid, effect, action, resource);
    }
}