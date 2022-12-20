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
package org.graylog.events.processor.systemnotification;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class TemplateRenderResponse {
    static final String FIELD_TITLE = "title";
    static final String FIELD_DESCRIPTION = "description";

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonCreator
    public static TemplateRenderResponse create(@JsonProperty(FIELD_TITLE) String title,
                                                @JsonProperty(FIELD_DESCRIPTION) String description) {
        return new AutoValue_TemplateRenderResponse(title, description);
    }
}
