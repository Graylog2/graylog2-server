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
package org.graylog2.indexer.indexset.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public interface IndexTemplateNameField {
    String FIELD_INDEX_TEMPLATE_NAME = "index_template_name";

    @JsonProperty(FIELD_INDEX_TEMPLATE_NAME)
    @NotBlank
    String indexTemplateName();

    interface IndexTemplateNameFieldBuilder<T> {

        @JsonProperty(IndexTemplateNameField.FIELD_INDEX_TEMPLATE_NAME)
        T indexTemplateName(String templateName);

    }
}
