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
package org.graylog2.inputs.categories;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nonnull;
import java.util.List;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = InputCategoryDto.Builder.class)
public abstract class InputCategoryDto implements MongoEntity {
    public static final String FIELD_ID = "id";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_SUBCATEGORY = "subcategory";
    public static final String FIELD_SUBCATEGORY_DESCRIPTION = "subcategory_description";
    public static final String FIELD_INPUT_TYPES = "input_types";
    public static final String FIELD_LICENSES = "licenses";

    @JsonProperty(FIELD_ID)
    @Id
    @ObjectId
    @Nonnull
    @Override
    public abstract String id();

    @JsonProperty(FIELD_CATEGORY)
    public abstract String category();

    @JsonProperty(FIELD_SUBCATEGORY)
    public abstract String subcategory();

    @JsonProperty(FIELD_SUBCATEGORY_DESCRIPTION)
    public abstract String subcategoryDescription();

    @JsonProperty(FIELD_INPUT_TYPES)
    public abstract List<String> inputTypes();

    @JsonProperty(FIELD_LICENSES)
    public abstract List<String> licenses();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_InputCategoryDto.Builder();
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_CATEGORY)
        public abstract Builder category(String category);

        @JsonProperty(FIELD_SUBCATEGORY)
        public abstract Builder subcategory(String subcategory);

        @JsonProperty(FIELD_SUBCATEGORY_DESCRIPTION)
        public abstract Builder subcategoryDescription(String subcategoryDescription);

        @JsonProperty(FIELD_INPUT_TYPES)
        public abstract Builder inputTypes(List<String> inputTypes);

        @JsonProperty(FIELD_LICENSES)
        public abstract Builder licenses(List<String> licenses);

        public abstract InputCategoryDto build();
    }
}
