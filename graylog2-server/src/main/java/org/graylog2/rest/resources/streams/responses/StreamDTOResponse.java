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
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static org.graylog2.streams.StreamImpl.FIELD_CATEGORIES;
import static org.graylog2.streams.StreamImpl.FIELD_CONTENT_PACK;
import static org.graylog2.streams.StreamImpl.FIELD_CREATED_AT;
import static org.graylog2.streams.StreamImpl.FIELD_CREATOR_USER_ID;
import static org.graylog2.streams.StreamImpl.FIELD_DESCRIPTION;
import static org.graylog2.streams.StreamImpl.FIELD_DISABLED;
import static org.graylog2.streams.StreamImpl.FIELD_INDEX_SET_ID;
import static org.graylog2.streams.StreamImpl.FIELD_IS_EDITABLE;
import static org.graylog2.streams.StreamImpl.FIELD_MATCHING_TYPE;
import static org.graylog2.streams.StreamImpl.FIELD_OUTPUTS;
import static org.graylog2.streams.StreamImpl.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM;
import static org.graylog2.streams.StreamImpl.FIELD_RULES;
import static org.graylog2.streams.StreamImpl.FIELD_TITLE;

public record StreamDTOResponse(@JsonProperty("id") String id,
                                @JsonProperty(FIELD_CREATOR_USER_ID) String creatorUserId,
                                @JsonProperty(FIELD_OUTPUTS) @Nullable Collection<ObjectId> outputs,
                                @JsonProperty(FIELD_MATCHING_TYPE) Stream.MatchingType matchingType,
                                @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
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
