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

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.configuration.converters.JavaDurationConverter;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public class ContentStreamConfiguration {
    @Parameter(value = "content_stream_rss_url")
    private URI contentStreamRssUri = URI.create("https://graylog.org/post/tag");

    @Parameter(value = "content_stream_refresh_interval",
               converter = JavaDurationConverter.class)
    private Duration contentStreamRefreshInterval = Duration.ofDays(7);

    public URI getContentStreamRssUri() {
        return contentStreamRssUri;
    }

    public Duration getContentStreamRefreshInterval() {
        return contentStreamRefreshInterval;
    }

    public Map<String, ?> contentStreamFrontendSettings() {
        return Map.of(
                "rss_url", getContentStreamRssUri(),
                "refresh_interval", getContentStreamRefreshInterval()
        );
    }

}
