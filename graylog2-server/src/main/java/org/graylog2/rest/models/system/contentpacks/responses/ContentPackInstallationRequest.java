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
package org.graylog2.rest.models.system.contentpacks.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ContentPackInstallationRequest {
    @JsonProperty("parameters")
    public abstract Map<String, ValueReference> parameters();

    @JsonProperty("comment")
    @Nullable
    public abstract String comment();

    @JsonCreator
    public static ContentPackInstallationRequest create(
            @JsonProperty("parameters") @Nullable Map<String, ValueReference> parameters,
            @JsonProperty("comment") @Nullable String comment) {
        final Map<String, ValueReference> parameterMap = parameters == null ? Collections.emptyMap() : parameters;
        return new AutoValue_ContentPackInstallationRequest(parameterMap, comment);
    }
}
