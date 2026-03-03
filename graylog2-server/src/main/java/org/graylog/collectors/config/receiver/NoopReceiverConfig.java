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
import com.google.auto.value.AutoValue;

/**
 * No-op receiver that we use to ensure the presence of at least one receiver in the Collector config.
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class NoopReceiverConfig implements CollectorReceiverConfig {
    public String type() {
        return "nop";
    }

    public static NoopReceiverConfig instance() {
        return new AutoValue_NoopReceiverConfig("nop");
    }
}
