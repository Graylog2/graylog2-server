package org.graylog2.datatier.tier.hot;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.datatier.tier.DataTier;
import org.graylog2.datatier.tier.DataTierType;
import org.joda.time.Period;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = HotTier.Builder.class)
public abstract class HotTier implements DataTier {


    public static final String INDEX_LIFETIME_MIN = "index_lifetime_min";
    public static final String INDEX_LIFETIME_MAX = "index_lifetime_max";

    public static final Period DEFAULT_LIFETIME_MIN = Period.days(30);
    public static final Period DEFAULT_LIFETIME_MAX = Period.days(40);

    public static HotTier.Builder builder() {
        return HotTier.Builder.create();
    }

    @Override
    public DataTierType getTier() {
        return DataTierType.HOT;
    }

    @Override
    public String getType() {
        return "HOT";
    }

    @JsonProperty(INDEX_LIFETIME_MIN)
    @Override
    public abstract Period indexLifetimeMin();

    @JsonProperty(INDEX_LIFETIME_MAX)
    @Override
    public abstract Period indexLifetimeMax();


    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static HotTier.Builder create() {
            return new $AutoValue_HotTier.Builder()
                    .indexLifetimeMin(DEFAULT_LIFETIME_MIN)
                    .indexLifetimeMax(DEFAULT_LIFETIME_MAX);
        }

        @JsonProperty(INDEX_LIFETIME_MIN)
        public abstract HotTier.Builder indexLifetimeMin(Period min);

        @JsonProperty(INDEX_LIFETIME_MAX)
        public abstract HotTier.Builder indexLifetimeMax(Period max);

        public abstract HotTier build();
    }
}
