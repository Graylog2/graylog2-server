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
package org.graylog2.i18n.feedback;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

/**
 * A report of a wrong, awkward or missing translation, created by a proofreader in the web interface.
 * Reports without a namespace and key are texts which aren't translated at all.
 */
public record TranslationFeedbackDTO(@JsonProperty(FIELD_ID) @Nullable @Id @ObjectId String id,
                                     @JsonProperty(FIELD_LANGUAGE) String language,
                                     @JsonProperty(FIELD_NAMESPACE) @Nullable String namespace,
                                     @JsonProperty(FIELD_KEY) @Nullable String key,
                                     @JsonProperty(FIELD_TEXT) String text,
                                     @JsonProperty(FIELD_ENGLISH_TEXT) @Nullable String englishText,
                                     @JsonProperty(FIELD_SUGGESTION) @Nullable String suggestion,
                                     @JsonProperty(FIELD_COMMENT) @Nullable String comment,
                                     @JsonProperty(FIELD_CATEGORY) String category,
                                     @JsonProperty(FIELD_PATH) @Nullable String path,
                                     @JsonProperty(FIELD_DOM_PATH) @Nullable String domPath,
                                     @JsonProperty(FIELD_REPORTER) String reporter,
                                     @JsonProperty(FIELD_STATUS) String status,
                                     @JsonProperty(FIELD_RESOLUTION) @Nullable String resolution,
                                     @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
                                     @JsonProperty(FIELD_UPDATED_AT) DateTime updatedAt) implements MongoEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_LANGUAGE = "language";
    public static final String FIELD_NAMESPACE = "namespace";
    public static final String FIELD_KEY = "key";
    public static final String FIELD_TEXT = "text";
    public static final String FIELD_ENGLISH_TEXT = "english_text";
    public static final String FIELD_SUGGESTION = "suggestion";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_DOM_PATH = "dom_path";
    public static final String FIELD_REPORTER = "reporter";
    public static final String FIELD_STATUS = "status";
    /** Note for the reporter how the report was handled, e.g. the new translation or why it stays as it is. */
    public static final String FIELD_RESOLUTION = "resolution";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_UPDATED_AT = "updated_at";

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_FIXED = "fixed";
    public static final String STATUS_WONT_FIX = "wont_fix";

    @JsonIgnore
    public boolean isUntranslated() {
        return namespace == null || key == null;
    }
}
