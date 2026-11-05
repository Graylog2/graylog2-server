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
package org.graylog2.i18n.feedback;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;
import java.util.List;

/**
 * How reports were handled, uploaded as a file after the translations were fixed, e.g.
 * <pre>{"resolutions": [{"id": "...", "status": "fixed", "note": "Changed to „Suche“"}]}</pre>
 * Other properties of the file, like a description or the date it was created, are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TranslationFeedbackResolutions(@JsonProperty("resolutions") @NotEmpty @Size(max = 10000) List<@Valid Resolution> resolutions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resolution(@JsonProperty("id") @NotBlank String id,
                             @JsonProperty(TranslationFeedbackDTO.FIELD_STATUS) @NotBlank String status,
                             @JsonProperty("note") @Nullable @Size(max = 5000) String note) {
    }
}
