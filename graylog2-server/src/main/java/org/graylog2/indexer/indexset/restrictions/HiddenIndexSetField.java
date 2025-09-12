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
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = HiddenIndexSetField.Builder.class)
public abstract class HiddenIndexSetField implements IndexSetFieldRestriction, FieldRestrictionValidator {

    public static final String TYPE_NAME = "hidden";

    public static Builder builder() {
        return AutoValue_HiddenIndexSetField.Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements IndexSetFieldRestrictionBuilder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_HiddenIndexSetField.Builder()
                    .type(TYPE_NAME);
        }

        public abstract HiddenIndexSetField build();
    }
}

