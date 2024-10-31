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
package org.graylog2.plugin.certificates;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public record RenewalPolicy(@JsonProperty("mode") @NotNull Mode mode,
                            @JsonProperty("certificate_lifetime") @NotNull String certificateLifetime) {

    private final static long CERT_RENEWAL_THRESHOLD_PERCENTAGE = 10;

    public enum Mode {
        AUTOMATIC,
        MANUAL;

        @JsonCreator
        public static Mode create(String value) {
            return Mode.valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public @NotNull Duration parsedCertificateLifetime() {
        return safeParse(certificateLifetime);
    }

    @JsonIgnore
    public Duration getRenewalThreshold() {
        return safeParse(certificateLifetime).dividedBy(CERT_RENEWAL_THRESHOLD_PERCENTAGE);
    }

    private Duration safeParse(String duration) {
        try {
            return Duration.parse(duration);
        } catch (DateTimeParseException ignored) {
            return periodToDuration(Period.parse(duration));
        }
    }

    private Duration periodToDuration(Period period) {
        return Duration.ofDays(period.getYears() * 365L + period.getMonths() * 30L + period.getDays());
    }

}
