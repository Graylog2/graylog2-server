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
package org.graylog2.indexer.indexset.template.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;

import javax.annotation.Nonnull;

import static org.graylog2.indexer.indexset.template.IndexSetTemplate.BUILT_IN_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.DESCRIPTION_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.ID_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.INDEX_SET_CONFIG_FIELD_NAME;
import static org.graylog2.indexer.indexset.template.IndexSetTemplate.TITLE_FIELD_NAME;


public record IndexSetTemplateResponse(@JsonProperty(ID_FIELD_NAME) @Nonnull String id,
                                       @JsonProperty(TITLE_FIELD_NAME) @Nonnull String title,
                                       @JsonProperty(DESCRIPTION_FIELD_NAME) String description,
                                       @JsonProperty(BUILT_IN_FIELD_NAME) boolean isBuiltIn,
                                       @JsonProperty(DEFAULT_FIELD_NAME) boolean isDefault,
                                       @JsonProperty(ENABLED_FIELD_NAME) boolean enabled,
                                       @JsonProperty(DISABLED_REASON_FIELD_NAME) String disabledReason,
                                       @JsonProperty(INDEX_SET_CONFIG_FIELD_NAME) @Nonnull IndexSetTemplateConfig indexSetConfig) {

    public static final String DEFAULT_FIELD_NAME = "default";
    public static final String ENABLED_FIELD_NAME = "enabled";
    public static final String DISABLED_REASON_FIELD_NAME = "disabled_reason";

}
