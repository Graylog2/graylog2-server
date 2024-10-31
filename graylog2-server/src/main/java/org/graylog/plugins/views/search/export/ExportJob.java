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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = ExportJob.FIELD_TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MessagesRequestExportJob.class, name = MessagesRequestExportJob.TYPE),
        @JsonSubTypes.Type(value = SearchExportJob.class, name = SearchExportJob.TYPE),
        @JsonSubTypes.Type(value = SearchTypeExportJob.class, name = SearchTypeExportJob.TYPE)
})
public interface ExportJob extends MongoEntity {
    String FIELD_TYPE = "type";
    String FIELD_ID = "_id";
    String FIELD_CREATED_AT = "created_at";

    @Id
    @ObjectId
    @JsonProperty(FIELD_ID)
    String id();

    @JsonProperty(FIELD_TYPE)
    String type();

    @JsonProperty(FIELD_CREATED_AT)
    DateTime createdAt();
}
