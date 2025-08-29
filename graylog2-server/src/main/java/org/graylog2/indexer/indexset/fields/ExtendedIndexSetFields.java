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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.graylog2.shared.fields.TitleAndDescriptionFields;
import org.graylog2.validation.SizeInBytes;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface ExtendedIndexSetFields extends
        TitleAndDescriptionFields,
        BaseIndexSetFields,
        WritableField,
        IndexAnalyzerField,
        FieldTypeProfileField {

    String FIELD_INDEX_PREFIX = "index_prefix";
    String FIELD_CREATION_DATE = "creation_date";
    String FIELD_INDEX_TEMPLATE_TYPE = "index_template_type";
    String INDEX_PREFIX_REGEX = "^[a-z0-9][a-z0-9_+-]*$";

    @JsonProperty(FIELD_INDEX_PREFIX)
    @NotBlank
    @Pattern(regexp = INDEX_PREFIX_REGEX)
    @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
    String indexPrefix();

    @JsonProperty(FIELD_CREATION_DATE)
    @NotNull
    ZonedDateTime creationDate();

    @JsonProperty(FIELD_INDEX_TEMPLATE_TYPE)
    Optional<String> indexTemplateType();

    interface ExtendedIndexSetFieldsBuilder<T> extends
            TitleAndDescriptionFieldsBuilder<T>,
            BaseIndexSetFieldsBuilder<T>,
            WritableFieldBuilder<T>,
            IndexAnalyzerFieldBuilder<T>,
            FieldTypeProfileFieldBuilder<T> {

        @JsonProperty(FIELD_INDEX_PREFIX)
        T indexPrefix(@NotBlank
                      @Pattern(regexp = INDEX_PREFIX_REGEX)
                      @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
                      String indexPrefix);

        @JsonProperty(FIELD_CREATION_DATE)
        T creationDate(@NotNull ZonedDateTime creationDate);

        @JsonProperty(FIELD_INDEX_TEMPLATE_TYPE)
        T indexTemplateType(@Nullable String templateType);
    }
}
