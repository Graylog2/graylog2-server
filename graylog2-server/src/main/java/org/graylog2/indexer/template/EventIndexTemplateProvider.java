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
package org.graylog2.indexer.template;

import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import org.graylog2.configuration.ElasticsearchConfiguration;

import static org.graylog2.shared.utilities.StringUtils.f;

public class EventIndexTemplateProvider extends BasicIndexTemplateProvider<EventsIndexMapping> {

    public static final String EVENT_TEMPLATE_TYPE = "events";

    private final String indexRefreshInterval;

    @Inject
    public EventIndexTemplateProvider(ElasticsearchConfiguration elasticsearchConfiguration) {
        this.indexRefreshInterval = toSearchTimeString(elasticsearchConfiguration.getEventsIndexRefreshInterval());
    }

    @Override
    protected EventsIndexMapping createTemplateInstance() {
        return new EventsIndexMapping7(indexRefreshInterval);
    }

    private static String toSearchTimeString(Duration duration) {
        final String unit = switch (duration.getUnit()) {
            case NANOSECONDS -> "nanos";
            case MICROSECONDS -> "micros";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "m";
            case HOURS -> "h";
            case DAYS -> "d";
        };
        return f("%d%s", duration.getQuantity(), unit);
    }

}
