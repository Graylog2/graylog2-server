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
package org.graylog2.indexer.indexset.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents template as it is stored in Mongo.
 */
public record IndexSetTemplate(@JsonProperty(ID_FIELD_NAME) @Nullable @Id @ObjectId String id,
                               @JsonProperty(TITLE_FIELD_NAME) @Nonnull String title,
                               @JsonProperty(DESCRIPTION_FIELD_NAME) String description,
                               @JsonProperty(READ_ONLY_FIELD_NAME) boolean isReadOnly,
                               @JsonProperty(INDEX_SET_CONFIG_FIELD_NAME) @Nonnull IndexSetsDefaultConfiguration indexSetConfig) {

    public static final String ID_FIELD_NAME = "id";
    public static final String TITLE_FIELD_NAME = "title";
    public static final String DESCRIPTION_FIELD_NAME = "description";
    public static final String READ_ONLY_FIELD_NAME = "read_only";
    public static final String INDEX_SET_CONFIG_FIELD_NAME = "index_set_config";

    public IndexSetTemplate(final IndexSetTemplateData data) {
        this(null, data.title(), data.description(), data.readOnly(), data.indexSetConfig());
    }

    public IndexSetTemplate(final IndexSetTemplateData data, boolean isReadOnly) {
        this(null, data.title(), data.description(), isReadOnly, data.indexSetConfig());
    }
}
