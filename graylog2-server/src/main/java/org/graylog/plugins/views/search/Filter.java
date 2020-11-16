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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = Filter.TYPE_FIELD,
        visible = true,
        defaultImpl = Filter.Fallback.class)
@JsonAutoDetect
public interface Filter {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    @JsonProperty("filters")
    Set<Filter> filters();

    Builder toGenericBuilder();

    interface Builder {
        public abstract Builder filters(Set<Filter> filters);

        public abstract Filter build();
    }

    @JsonAutoDetect
    class Fallback implements Filter {
        @JsonProperty
        private String type;

        @Override
        public String type() {
            return type;
        }

        @Override
        public Set<Filter> filters() {
            return Collections.emptySet();
        }

        @JsonAnySetter
        public void setType(String key, Object value) {
            // we ignore all the other values, we only want to be able to deserialize unknown filters
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Fallback fallback = (Fallback) o;
            return Objects.equals(type, fallback.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public Builder toGenericBuilder() {
            return null;
        }
    }
}
