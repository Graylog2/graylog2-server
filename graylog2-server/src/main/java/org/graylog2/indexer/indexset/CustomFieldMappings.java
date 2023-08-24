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
package org.graylog2.indexer.indexset;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CustomFieldMappings extends HashSet<CustomFieldMapping> {

    public CustomFieldMappings() {
        super();
    }

    public CustomFieldMappings(final Collection<CustomFieldMapping> mappings) {
        super(mappings);
    }

    public CustomFieldMappings mergeWith(final CustomFieldMapping changedMapping) {
        final Set<CustomFieldMapping> modifiedMappings = new HashSet<>(this);
        modifiedMappings.removeIf(m -> changedMapping.fieldName().equals(m.fieldName()));
        modifiedMappings.add(changedMapping);
        return new CustomFieldMappings(modifiedMappings);
    }
}
