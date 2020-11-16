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
package org.graylog2.indexer.fieldtypes;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for field type lookups.
 */
public interface FieldTypeLookup {
    /**
     * Returns the {@link FieldTypes} object for the given message field name.
     *
     * @param fieldName name of the field to get the type for
     * @return field type object
     */
    Optional<FieldTypes> get(String fieldName);

    /**
     * Returns a map of field names to {@link FieldTypes} objects.
     *
     * @param fieldNames a collection of field names to get the types for
     * @return map of field names to field type objects
     */
    Map<String, FieldTypes> get(Collection<String> fieldNames);

    /**
     * Returns a map of field names to the corresponding field types.
     *
     * @param fieldNames a collection of field names to get the types for
     * @param indexNames a collection of index names to filter the results
     * @return map of field names to field type objects
     */
    Map<String, FieldTypes> get(Collection<String> fieldNames, Collection<String> indexNames);
}
