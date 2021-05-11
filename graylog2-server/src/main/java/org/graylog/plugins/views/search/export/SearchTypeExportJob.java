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
package org.graylog.plugins.views.search.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.constraints.NotEmpty;

@AutoValue
@JsonAutoDetect
public abstract class SearchTypeExportJob implements ExportJob {
    static final String TYPE = "search_type_export";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_SEARCH_TYPE_ID = "search_type_id";
    private static final String FIELD_RESULT_FORMAT = "result_format";

    @JsonProperty("type")
    public String type() {
        return TYPE;
    }

    @JsonProperty(FIELD_SEARCH_ID)
    public abstract String searchId();

    @JsonProperty(FIELD_SEARCH_TYPE_ID)
    public abstract String searchTypeId();

    @JsonProperty(FIELD_RESULT_FORMAT)
    @NotEmpty
    public abstract ResultFormat resultFormat();

    static SearchTypeExportJob forSearchType(String id, String searchId, String searchTypeId, ResultFormat resultFormat) {
        return new AutoValue_SearchTypeExportJob(id, DateTime.now(DateTimeZone.UTC), searchId, searchTypeId, resultFormat);
    }

    @JsonCreator
    static SearchTypeExportJob create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_SEARCH_ID) String searchId,
            @JsonProperty(FIELD_SEARCH_TYPE_ID) String searchTypeId,
            @JsonProperty(FIELD_RESULT_FORMAT) ResultFormat resultFormat
    ) {
        return new AutoValue_SearchTypeExportJob(id,
                DateTime.now(DateTimeZone.UTC),
                searchId,
                searchTypeId,
                resultFormat
        );
    }
}
