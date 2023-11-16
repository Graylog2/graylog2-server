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
