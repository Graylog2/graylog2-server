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
package org.graylog.integrations.pagerduty.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;

/**
 * @author Edgar Molina
 *
 */
public class Link {
    @JsonProperty("href")
    private final URL href;
    @JsonProperty("text")
    private final String text;

    public Link(URL href, String text) {
        this.href = href;
        this.text = text;
    }

    public URL getHref() {
        return href;
    }

    public String getText() {
        return text;
    }
}
