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
package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.graylog2.streams.StreamDTO.FIELD_CATEGORIES;
import static org.graylog2.streams.StreamDTO.FIELD_CONTENT_PACK;
import static org.graylog2.streams.StreamDTO.FIELD_CREATED_AT;
import static org.graylog2.streams.StreamDTO.FIELD_CREATOR_USER_ID;
import static org.graylog2.streams.StreamDTO.FIELD_DESCRIPTION;
import static org.graylog2.streams.StreamDTO.FIELD_DISABLED;
import static org.graylog2.streams.StreamDTO.FIELD_INDEX_SET_ID;
import static org.graylog2.streams.StreamDTO.FIELD_IS_EDITABLE;
import static org.graylog2.streams.StreamDTO.FIELD_MATCHING_TYPE;
import static org.graylog2.streams.StreamDTO.FIELD_OUTPUTS;
import static org.graylog2.streams.StreamDTO.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM;
import static org.graylog2.streams.StreamDTO.FIELD_RULES;
import static org.graylog2.streams.StreamDTO.FIELD_TITLE;

public record StreamDTOResponse(@JsonProperty("id") String id,
                                @JsonProperty(FIELD_CREATOR_USER_ID) String creatorUserId,
                                @JsonProperty(FIELD_OUTPUTS) @Nullable Collection<ObjectId> outputs,
                                @JsonProperty(FIELD_MATCHING_TYPE) String matchingType,
                                @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                @JsonProperty(FIELD_CREATED_AT) Date createdAt,
                                @JsonProperty(FIELD_RULES) @Nullable Collection<StreamRule> rules,
                                @JsonProperty(FIELD_DISABLED) boolean disabled,
                                @JsonProperty(FIELD_TITLE) String title,
                                @JsonProperty(FIELD_CONTENT_PACK) @Nullable String contentPack,
                                @JsonProperty("is_default") @Nullable Boolean isDefault,
                                @JsonProperty(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM) @Nullable Boolean removeMatchesFromDefaultStream,
                                @JsonProperty(FIELD_INDEX_SET_ID) String indexSetId,
                                @JsonProperty(FIELD_IS_EDITABLE) boolean isEditable,
                                @JsonProperty(FIELD_CATEGORIES) List<String> categories) {
}
