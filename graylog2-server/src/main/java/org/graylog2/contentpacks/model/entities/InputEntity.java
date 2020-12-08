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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.Reference;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class InputEntity {
    @JsonProperty("title")
    @NotBlank
    public abstract ValueReference title();

    @JsonProperty("configuration")
    @NotNull
    public abstract ReferenceMap configuration();

    @JsonProperty("static_fields")
    @NotNull
    public abstract Map<String, ValueReference> staticFields();

    @JsonProperty("type")
    @NotBlank
    public abstract ValueReference type();

    @JsonProperty("global")
    public abstract ValueReference global();

    @JsonProperty("extractors")
    @NotNull
    public abstract List<ExtractorEntity> extractors();

    @JsonCreator
    public static InputEntity create(
            @JsonProperty("title") @NotBlank ValueReference title,
            @JsonProperty("configuration") @NotNull ReferenceMap configuration,
            @JsonProperty("static_fields") @NotNull Map<String, ValueReference> staticFields,
            @JsonProperty("type") @NotBlank ValueReference type,
            @JsonProperty("global") ValueReference global,
            @JsonProperty("extractors") @NotNull List<ExtractorEntity> extractors) {
        return new AutoValue_InputEntity(title, configuration, staticFields, type, global, extractors);
    }
}
