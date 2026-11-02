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
package org.graylog2.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.configuration.tunables.RuntimeTunable;
import org.graylog2.configuration.tunables.TunableField;

@RuntimeTunable(id = "welcome_page.configuration", feature = "welcome_page", title = "Welcome Page",
        description = "The welcome page can show widgets with message, alert, and event counts, plus a top sources chart. "
                + "On large or busy clusters, these queries add extra load every time a user opens the welcome page. "
                + "Disable them below if you want to avoid that load. The widgets will be hidden and their queries will not run.")
public record WelcomePageConfiguration(
        @JsonProperty("disable_metrics")
        @TunableField(label = "Disable welcome page metrics",
                help = "If enabled, the message, alert, and event count widgets and the top sources chart will be hidden from the welcome page, "
                        + "and their underlying queries will not run. Turn this on for large or busy clusters to reduce query load.")
        boolean disableMetrics
) {
    public static final WelcomePageConfiguration DEFAULT = new WelcomePageConfiguration(false);

    public static WelcomePageConfiguration defaults() {
        return DEFAULT;
    }
}
