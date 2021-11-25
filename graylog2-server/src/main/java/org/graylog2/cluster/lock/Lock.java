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
package org.graylog2.cluster.lock;

import com.google.auto.value.AutoValue;

import java.time.ZonedDateTime;

@AutoValue
public abstract class Lock {
    static final String FIELD_RESOURCE = "resource";
    static final String FIELD_LOCKED_BY = "locked_by";
    static final String FIELD_UPDATED_AT = "updated_at";

    public abstract String resource();

    public abstract String lockedBy();

    public abstract ZonedDateTime createdAt();

    public abstract ZonedDateTime updatedAt();

    public static Builder builder() {
        return new AutoValue_Lock.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder resource(String resource);

        public abstract Builder lockedBy(String lockedBy);

        public abstract Builder createdAt(ZonedDateTime createdAt);

        public abstract Builder updatedAt(ZonedDateTime updatedAt);

        public abstract Lock build();
    }
}
