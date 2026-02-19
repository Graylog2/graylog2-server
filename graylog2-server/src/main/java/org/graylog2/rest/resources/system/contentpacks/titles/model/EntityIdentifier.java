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
package org.graylog2.rest.resources.system.contentpacks.titles.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.search.SearchQueryField;

import javax.annotation.Nullable;
import java.util.List;

public record EntityIdentifier(@JsonProperty(value = "id", required = true) String id,
                               @JsonProperty(value = "type", required = true) String type,
                               @JsonProperty("identifier_field") @Nullable String identifierField,
                               @JsonProperty("identifier_type") @Nullable String identifierType,
                               @JsonProperty("display_fields") @Nullable List<String> displayFields,
                               @JsonProperty("display_template") @Nullable String displayTemplate) {

    public EntityIdentifier(String id, String type) {
        this(id, type, null, null, null, null);
    }

    public String effectiveIdentifierField() {
        return identifierField != null ? identifierField : "_id";
    }

    public SearchQueryField.Type effectiveIdentifierType() {
        if (identifierType == null) {
            return SearchQueryField.Type.OBJECT_ID;
        }
        return SearchQueryField.Type.valueOf(identifierType);
    }
}
