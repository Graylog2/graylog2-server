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
package org.graylog2.indexer.indexset.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents profile as it is stored in Mongo.
 */
public record IndexFieldTypeProfile(@JsonProperty(ID_FIELD_NAME) @Nullable @Id @ObjectId String id,
                                    @JsonProperty(NAME_FIELD_NAME) String name,
                                    @JsonProperty(DESCRIPTION_FIELD_NAME) String description,
                                    @JsonProperty(CUSTOM_MAPPINGS_FIELD_NAME) @Nonnull CustomFieldMappings customFieldMappings) {

    public static final String ID_FIELD_NAME = "id";
    public static final String NAME_FIELD_NAME = "name";
    public static final String DESCRIPTION_FIELD_NAME = "description";
    public static final String CUSTOM_MAPPINGS_FIELD_NAME = "custom_field_mappings";

    public IndexFieldTypeProfile(final IndexFieldTypeProfileData data) {
        this(null, data.name(), data.description(), data.customFieldMappings());
    }
}
