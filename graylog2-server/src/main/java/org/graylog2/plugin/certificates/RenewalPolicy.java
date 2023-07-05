package org.graylog2.plugin.certificates;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Locale;

public record RenewalPolicy(@JsonProperty("mode") @NotNull Mode mode,
                            @JsonProperty("certificate_lifetime") @NotNull String certificateLifetime) {
    public enum Mode {
        AUTOMATIC,
        MANUAL;

        @JsonCreator
        public static Mode create(String value) {
            return Mode.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }
}
