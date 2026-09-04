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

import javax.annotation.Nullable;
import java.util.Optional;

public interface IndexTemplateTypeField {
    String FIELD_INDEX_TEMPLATE_TYPE = "index_template_type";

    @JsonProperty(IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE)
    Optional<String> indexTemplateType();

    interface IndexTemplateTypeFieldBuilder<T> {

        @JsonProperty(IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE)
        T indexTemplateType(@Nullable String templateType);
    }
}
