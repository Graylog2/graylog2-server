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
package org.graylog2.rest.models.messages.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonTypeName("ChangedField")
@JsonDeserialize(builder = AutoValue_ChangedField.Builder.class)
public abstract class ChangedField {

    public static final String FIELD_BEFORE = "before";
    public static final String FIELD_AFTER = "after";

    @JsonProperty(FIELD_BEFORE)
    public abstract Object before();

    @JsonProperty(FIELD_AFTER)
    public abstract Object after();

    public static ChangedField.Builder builder() {
        return new AutoValue_ChangedField.Builder();
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder {

        @JsonProperty(FIELD_BEFORE)
        public abstract ChangedField.Builder before(Object before);

        @JsonProperty(FIELD_AFTER)
        public abstract ChangedField.Builder after(Object before);

        public abstract ChangedField build();
    }
}
