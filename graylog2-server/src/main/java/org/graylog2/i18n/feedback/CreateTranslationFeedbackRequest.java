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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;

import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_CATEGORY;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_COMMENT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_DOM_PATH;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_ENGLISH_TEXT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_KEY;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_LANGUAGE;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_NAMESPACE;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_PATH;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_SUGGESTION;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_TEXT;

public record CreateTranslationFeedbackRequest(@JsonProperty(FIELD_LANGUAGE) @NotBlank @Size(max = 20) String language,
                                               @JsonProperty(FIELD_NAMESPACE) @Nullable @Size(max = 200) String namespace,
                                               @JsonProperty(FIELD_KEY) @Nullable @Size(max = 500) String key,
                                               @JsonProperty(FIELD_TEXT) @NotBlank @Size(max = 5000) String text,
                                               @JsonProperty(FIELD_ENGLISH_TEXT) @Nullable @Size(max = 5000) String englishText,
                                               @JsonProperty(FIELD_SUGGESTION) @Nullable @Size(max = 5000) String suggestion,
                                               @JsonProperty(FIELD_COMMENT) @Nullable @Size(max = 5000) String comment,
                                               @JsonProperty(FIELD_CATEGORY) @NotBlank @Size(max = 50) String category,
                                               @JsonProperty(FIELD_PATH) @Nullable @Size(max = 2000) String path,
                                               @JsonProperty(FIELD_DOM_PATH) @Nullable @Size(max = 2000) String domPath) {
}
