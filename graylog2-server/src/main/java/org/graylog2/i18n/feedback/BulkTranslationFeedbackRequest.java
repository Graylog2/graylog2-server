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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Reports to change at once. The status is only needed to change the status, not to delete.
 */
public record BulkTranslationFeedbackRequest(@JsonProperty("ids") @NotEmpty @Size(max = 10000) List<String> ids,
                                             @JsonProperty(TranslationFeedbackDTO.FIELD_STATUS) @Nullable String status) {
}
