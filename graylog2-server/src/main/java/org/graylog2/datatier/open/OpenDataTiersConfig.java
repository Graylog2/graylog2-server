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
package org.graylog2.datatier.open;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.datatier.common.tier.HotTierConfig;
import org.graylog2.datatier.DataTiersConfig;

import javax.validation.constraints.NotNull;


@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = OpenDataTiersConfig.Builder.class)
public abstract class OpenDataTiersConfig implements DataTiersConfig{

    public final static String TYPE_OPEN = OpenDataTiersConfig.class.getCanonicalName();

    public static Builder builder() {
        return Builder.create();
    }

    @NotNull
    @JsonProperty(DataTiersConfig.FIELD_HOT_TIER)
    public abstract HotTierConfig hotTier();


    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_OpenDataTiersConfig.Builder()
                    .type(TYPE_OPEN);
        }

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(@NotNull String type);

        @JsonProperty(DataTiersConfig.FIELD_HOT_TIER)
        public abstract Builder hotTier(@NotNull HotTierConfig hotTier);

        public abstract OpenDataTiersConfig build();
    }
}
