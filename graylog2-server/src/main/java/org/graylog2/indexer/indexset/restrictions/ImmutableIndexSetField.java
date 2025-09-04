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
package org.graylog2.indexer.indexset.restrictions;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.jayway.jsonpath.DocumentContext;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Objects;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = ImmutableIndexSetField.Builder.class)
public abstract class ImmutableIndexSetField implements IndexSetFieldRestriction, FieldRestrictionValidator {

    public static final String TYPE_NAME = "immutable";

    @Override
    public boolean validate(String fieldName, DocumentContext doc1, DocumentContext doc2) {
        String path = "$." + fieldName;
        return Objects.equals(doc1.read(path), doc2.read(path));
    }

    public static Builder builder() {
        return AutoValue_ImmutableIndexSetField.Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements IndexSetFieldRestrictionBuilder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ImmutableIndexSetField.Builder()
                    .type(TYPE_NAME);
        }

        public abstract ImmutableIndexSetField build();
    }
}

