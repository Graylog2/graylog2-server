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
package org.graylog.plugins.views.search.export;

import com.google.auto.value.AutoValue;

import java.util.LinkedHashMap;

@AutoValue
public abstract class SimpleMessage {
    public static SimpleMessage from(String index, LinkedHashMap<String, Object> fieldsMap) {
        return builder().fields(fieldsMap).index(index).build();
    }

    public abstract LinkedHashMap<String, Object> fields();

    public abstract String index();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    public Object valueFor(String fieldName) {
        return fields().get(fieldName);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder fields(LinkedHashMap<String, Object> fields);

        public abstract Builder index(String index);

        public static Builder create() {
            return new AutoValue_SimpleMessage.Builder();
        }

        abstract SimpleMessage autoBuild();

        public SimpleMessage build() {
            return autoBuild();
        }
    }
}
