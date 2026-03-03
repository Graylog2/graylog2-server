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
package org.graylog.collectors.config.receiver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;

import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Otel collector journald receiver configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/journaldreceiver">journald receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class JournaldReceiverConfig implements CollectorReceiverConfig, CollectorStanzaReceiver {
    public static final String RECEIVER_TYPE = "journald";

    public enum StartAt {
        @JsonProperty("beginning")
        BEGINNING,
        @JsonProperty("end")
        END
    }

    public enum Priority {
        @JsonProperty("emerg")
        EMERG,
        @JsonProperty("alert")
        ALERT,
        @JsonProperty("crit")
        CRIT,
        @JsonProperty("err")
        ERR,
        @JsonProperty("warning")
        WARNING,
        @JsonProperty("notice")
        NOTICE,
        @JsonProperty("info")
        INFO,
        @JsonProperty("debug")
        DEBUG
    }

    public String type() {
        return RECEIVER_TYPE;
    }

    @JsonProperty("priority")
    public abstract Priority priority();

    @Nullable
    @JsonProperty("matches")
    public abstract List<String> matches();

    // Available options: "beginning", "end"
    @JsonProperty("start_at")
    public abstract StartAt startAt();

    public static Builder builder(String id) {
        return new AutoValue_JournaldReceiverConfig.Builder()
                .name(f("journald/%s", id))
                .startAt(StartAt.END)
                .priority(Priority.INFO);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder priority(Priority priority);

        public abstract Builder matches(@Nullable List<String> matches);

        public abstract Builder startAt(StartAt startAt);

        public abstract JournaldReceiverConfig build();
    }
}
