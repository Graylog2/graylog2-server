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

import javax.annotation.Nonnull;

import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.CUSTOM_MAPPINGS_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile.NAME_FIELD_NAME;

public record IndexFieldTypeProfileData(@JsonProperty(NAME_FIELD_NAME) String name,
                                        @JsonProperty(DESCRIPTION_FIELD_NAME) String description,
                                        @JsonProperty(CUSTOM_MAPPINGS_FIELD_NAME) @Nonnull CustomFieldMappings customFieldMappings) {
}
