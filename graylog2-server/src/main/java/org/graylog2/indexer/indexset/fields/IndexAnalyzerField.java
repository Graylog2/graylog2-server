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

public interface IndexAnalyzerField {
    String FIELD_INDEX_ANALYZER = "index_analyzer";

    @JsonProperty(FIELD_INDEX_ANALYZER)
    @NotBlank
    String indexAnalyzer();

    interface IndexAnalyzerFieldBuilder<T> {

        @JsonProperty(IndexAnalyzerField.FIELD_INDEX_ANALYZER)
        T indexAnalyzer(@NotBlank String indexAnalyzer);

    }
}
