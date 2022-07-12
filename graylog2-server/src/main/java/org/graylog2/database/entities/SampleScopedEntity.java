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
package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

// TODO: remove this sample class--added here only to test default scope options
@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = SampleScopedEntity.Builder.class)
@Deprecated
public abstract class SampleScopedEntity extends ScopedEntity {

    public abstract String title();

    @AutoValue.Builder
    public abstract static class Builder extends ScopedEntity.Builder<Builder> {

        public abstract Builder title(String title);

        public abstract SampleScopedEntity build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_SampleScopedEntity.Builder().scope(DefaultEntityScope.NAME);
        }
    }
}
