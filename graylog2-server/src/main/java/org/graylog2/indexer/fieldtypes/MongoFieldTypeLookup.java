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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This can be used to lookup types for message fields.
 */
public class MongoFieldTypeLookup implements FieldTypeLookup {
    private final IndexFieldTypesService dbService;
    private final FieldTypeMapper typeMapper;

    @Inject
    public MongoFieldTypeLookup(final IndexFieldTypesService dbService,
                                final FieldTypeMapper typeMapper) {
        this.dbService = dbService;
        this.typeMapper = typeMapper;
    }

    /**
     * Returns the {@link FieldTypes} object for the given message field name.
     *
     * @param fieldName name of the field to get the type for
     * @return field type object
     */
    @Override
    public Optional<FieldTypes> get(final String fieldName) {
        return Optional.ofNullable(get(ImmutableSet.of(fieldName)).get(fieldName));
    }

    /**
     * Returns a map of field names to {@link FieldTypes} objects.
     *
     * @param fieldNames a collection of field names to get the types for
     * @return map of field names to field type objects
     */
    @Override
    public Map<String, FieldTypes> get(final Collection<String> fieldNames) {
        return get(fieldNames, ImmutableSet.of());
    }

    /**
     * Returns a map of field names to the corresponding field types.
     *
     * @param fieldNames a collection of field names to get the types for
     * @param indexNames a collection of index names to filter the results
     * @return map of field names to field type objects
     */
    @Override
    public Map<String, FieldTypes> get(final Collection<String> fieldNames, Collection<String> indexNames) {
        // Shortcut - if we don't select any fields we don't have to do any database query
        if (fieldNames.isEmpty()) {
            return Collections.emptyMap();
        }

        // We have to transform the field type database entries to make them usable for the user.
        //
        // [
        //   {
        //     "index_name": "graylog_0",
        //     "fields": [
        //       {"field_name": "message", "physical_type": "text"},
        //       {"field_name": "source", "physical_type": "keyword"}
        //     ]
        //   },
        //   {
        //     "index_name": "graylog_1",
        //     "fields": [
        //       {"field_name": "message", "physical_type": "text"},
        //     ]
        //   }
        // ]
        //
        // gets transformed into
        //
        // {
        //   "message": {
        //     "field_name": "message",
        //     "types": [
        //       {
        //         "type": "string",
        //         "properties": ["full-text-search"],
        //         "index_names": ["graylog_0", "graylog_1"]
        //     ]
        //   },
        //   "source": {
        //     "field_name": "source",
        //     "types": [
        //       {
        //         "type": "string",
        //         "properties": ["enumerable"],
        //         "index_names": ["graylog_0"]
        //     ]
        //   }
        // }

        // field-name -> {physical-type -> [index-name, ...]}
        final Map<String, SetMultimap<String, String>> fields = new HashMap<>();

        // Convert the data from the database to be indexed by field name and physical type
        getTypesStream(fieldNames, indexNames).forEach(types -> {
            final String indexName = types.indexName();

            types.fields().forEach(fieldType -> {
                final String fieldName = fieldType.fieldName();
                final String physicalType = fieldType.physicalType();

                if (fieldNames.contains(fieldName)) {
                    if (indexNames.isEmpty() || indexNames.contains(indexName)) {
                        if (!fields.containsKey(fieldName)) {
                            fields.put(fieldName, HashMultimap.create());
                        }
                        fields.get(fieldName).put(physicalType, indexName);
                    }
                }
            });
        });

        final ImmutableMap.Builder<String, FieldTypes> result = ImmutableMap.builder();

        for (Map.Entry<String, SetMultimap<String, String>> fieldNameEntry : fields.entrySet()) {
            final String fieldName = fieldNameEntry.getKey();
            final Map<String, Collection<String>> physicalTypes = fieldNameEntry.getValue().asMap();

            // Use the field type mapper to do the conversion between the Elasticsearch type and our logical type
            final Set<FieldTypes.Type> types = physicalTypes.entrySet().stream()
                    .map(entry -> {
                        final String physicalType = entry.getKey();
                        final Set<String> indices = ImmutableSet.copyOf(entry.getValue());

                        return typeMapper.mapType(physicalType).map(t -> t.withIndexNames(indices));
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());

            result.put(fieldName, FieldTypes.create(fieldName, types));
        }

        return result.build();
    }

    private Collection<IndexFieldTypesDTO> getTypesStream(Collection<String> fieldNames, Collection<String> indexNames) {
        if (indexNames.isEmpty()) {
            return dbService.findForFieldNames(fieldNames);
        }

        return dbService.findForFieldNamesAndIndices(fieldNames, indexNames);
    }
}
