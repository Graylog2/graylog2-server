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
package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

/**
 * A common implementation on AWSRequest, which can be used for any AWS request that just needs region and credentials.
 */
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = AWSRequestImpl.Builder.class)
public abstract class AWSRequestImpl implements AWSRequest {

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_AWSRequestImpl.Builder();
        }

        public abstract AWSRequestImpl build();
    }
}