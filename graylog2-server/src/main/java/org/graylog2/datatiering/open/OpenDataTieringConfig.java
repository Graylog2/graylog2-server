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
package org.graylog2.datatiering.open;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.rotation.tso.IndexLifetimeConfig;
import org.joda.time.Period;

import javax.validation.constraints.NotNull;


@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = OpenDataTieringConfig.Builder.class)
public abstract class OpenDataTieringConfig implements DataTieringConfig {

    public final static String TYPE_OPEN = "data_tiering_open";

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_OpenDataTieringConfig.Builder()
                    .type(TYPE_OPEN);
        }

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(@NotNull String type);

        @JsonProperty(IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MIN)
        public abstract Builder indexLifetimeMin(Period indexLifetimeMin);

        @JsonProperty(IndexLifetimeConfig.FIELD_INDEX_LIFETIME_MAX)
        public abstract Builder indexLifetimeMax(Period indexLifetimeMax);


        public abstract OpenDataTieringConfig build();
    }
}
