package org.graylog2.datatier.tier;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.Period;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public interface DataTier {

    @JsonProperty("tier")
    @NotNull
    DataTierType getTier();

    @JsonProperty("type")
    @NotBlank
    String getType();

    Period indexLifetimeMax();

    Period indexLifetimeMin();
}
