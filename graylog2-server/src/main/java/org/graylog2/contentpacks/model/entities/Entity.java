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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.Typed;
import org.graylog2.contentpacks.model.Versioned;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Versioned.FIELD_META_VERSION)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EntityV1.class, name = EntityV1.VERSION)
})
public interface Entity extends Identified, Typed, Versioned {
    EntityDescriptor toEntityDescriptor();

    interface EntityBuilder<SELF> extends IdBuilder<SELF>, TypeBuilder<SELF>, VersionBuilder<SELF> {
    }
}
