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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.constraints.Constraint;

import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Versioned.FIELD_META_VERSION, defaultImpl = LegacyContentPack.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LegacyContentPack.class),
        @JsonSubTypes.Type(value = ContentPackV1.class, name = ContentPackV1.VERSION)
})
public interface ContentPack extends Identified, Revisioned, Versioned {
    interface ContentPackBuilder<SELF> extends IdBuilder<SELF>, RevisionBuilder<SELF>, VersionBuilder<SELF> {
    }
}
