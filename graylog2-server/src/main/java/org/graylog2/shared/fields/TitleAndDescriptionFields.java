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
package org.graylog2.shared.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public interface TitleAndDescriptionFields {
    String FIELD_TITLE = "title";
    String FIELD_DESCRIPTION = "description";

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    String title();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    String description();

    interface TitleAndDescriptionFieldsBuilder<T> {

        @JsonProperty(FIELD_TITLE)
        T title(@NotBlank String title);

        @JsonProperty(FIELD_DESCRIPTION)
        T description(String description);
    }
}
