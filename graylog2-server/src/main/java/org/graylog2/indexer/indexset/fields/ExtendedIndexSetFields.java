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
import jakarta.validation.constraints.NotNull;
import org.graylog2.shared.fields.TitleAndDescriptionFields;

import java.time.ZonedDateTime;

public interface ExtendedIndexSetFields extends
        TitleAndDescriptionFields,
        ShardsAndReplicasField,
        RotationAndRetentionFields,
        WritableField,
        IndexAnalyzerField,
        FieldTypeProfileField,
        IndexTemplateTypeField,
        IndexPrefixField {

    String FIELD_CREATION_DATE = "creation_date";

    @JsonProperty(FIELD_CREATION_DATE)
    @NotNull
    ZonedDateTime creationDate();


    interface ExtendedIndexSetFieldsBuilder<T> extends
            TitleAndDescriptionFieldsBuilder<T>,
            ShardsAndReplicasFieldBuilder<T>,
            RotationAndRetentionFieldsBuilder<T>,
            WritableFieldBuilder<T>,
            IndexAnalyzerFieldBuilder<T>,
            FieldTypeProfileFieldBuilder<T>,
            IndexTemplateTypeFieldBuilder<T>,
            IndexPrefixFieldBuilder<T> {

        @JsonProperty(FIELD_CREATION_DATE)
        T creationDate(@NotNull ZonedDateTime creationDate);

    }
}
