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
package org.graylog2.decorators;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.database.CollectionName;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@CollectionName("decorators")
public abstract class DecoratorImpl implements Decorator, Comparable {
    static final String FIELD_ID = "id";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";
    static final String FIELD_STREAM = "stream";
    static final String FIELD_ORDER = "order";

    @Override
    public int compareTo(Object o) {
        if (o instanceof Decorator) {
            Decorator decorator = (Decorator)o;
            return order() - decorator.order();
        }
        return 0;
    }

    @JsonProperty(FIELD_ID)
    @Id
    @ObjectId
    @Nullable
    @Override
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    @NotBlank
    @Override
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    @Override
    public abstract Map<String, Object> config();

    @JsonProperty(FIELD_STREAM)
    @Override
    public abstract Optional<String> stream();

    @JsonProperty(FIELD_ORDER)
    @Override
    public abstract int order();

    public abstract Builder toBuilder();

    @JsonCreator
    public static DecoratorImpl create(@JsonProperty(FIELD_ID) @Id @ObjectId @Nullable String id,
                                       @JsonProperty(FIELD_TYPE) String type,
                                       @JsonProperty(FIELD_CONFIG) Map<String, Object> config,
                                       @JsonProperty(FIELD_STREAM) Optional<String> stream,
                                       @JsonProperty(FIELD_ORDER) int order) {
        return new AutoValue_DecoratorImpl.Builder()
            .id(id)
            .type(type)
            .config(config)
            .stream(stream)
            .order(order)
            .build();
    }

    public static Decorator create(@JsonProperty(FIELD_TYPE) String type,
                                   @JsonProperty(FIELD_CONFIG) Map<String, Object> config,
                                   @JsonProperty(FIELD_STREAM) Optional<String> stream,
                                   @JsonProperty(FIELD_ORDER) int order) {
        return create(null, type, config, stream, order);
    }

    public static Decorator create(@JsonProperty(FIELD_TYPE) String type,
                                   @JsonProperty(FIELD_CONFIG) Map<String, Object> config,
                                   @JsonProperty(FIELD_ORDER) int order) {
        return create(type, config, Optional.empty(), order);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);
        abstract Builder type(String type);
        abstract Builder config(Map<String, Object> config);
        abstract Builder stream(Optional<String> stream);
        abstract Builder order(int order);
        public abstract DecoratorImpl build();
    }
}
