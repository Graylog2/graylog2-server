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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.inject.Singleton;
import org.graylog.events.event.EventDto;
import org.graylog.plugins.views.search.searchtypes.events.CommonEventSummary;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;

import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;

/**
 * Maps Elasticsearch field types to Graylog types.
 * <p>
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html">Elasticsearch mapping types</a>
 */
@Singleton
public class FieldTypeMapper {
    private static final String PROP_ENUMERABLE = "enumerable";
    public static final String PROP_FULL_TEXT_SEARCH = "full-text-search";
    public static final String PROP_NUMERIC = "numeric";

    public static final FieldTypes.Type STRING_TYPE = createType("string", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type STRING_FTS_TYPE = createType("string", of(PROP_FULL_TEXT_SEARCH));
    public static final FieldTypes.Type LONG_TYPE = createType("long", of(PROP_NUMERIC, PROP_ENUMERABLE));
    public static final FieldTypes.Type INT_TYPE = createType("int", of(PROP_NUMERIC, PROP_ENUMERABLE));
    public static final FieldTypes.Type SHORT_TYPE = createType("short", of(PROP_NUMERIC, PROP_ENUMERABLE));
    public static final FieldTypes.Type BYTE_TYPE = createType("byte", of(PROP_NUMERIC, PROP_ENUMERABLE));
    public static final FieldTypes.Type DOUBLE_TYPE = createType("double", of(PROP_NUMERIC, PROP_ENUMERABLE));
    public static final FieldTypes.Type FLOAT_TYPE = createType("float", of(PROP_NUMERIC, PROP_ENUMERABLE));
    public static final FieldTypes.Type DATE_TYPE = createType("date", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type BOOLEAN_TYPE = createType("boolean", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type BINARY_TYPE = createType("binary", of());
    public static final FieldTypes.Type GEO_POINT_TYPE = createType("geo-point", of());
    public static final FieldTypes.Type IP_TYPE = createType("ip", of(PROP_ENUMERABLE));

    /* Content-specific field types */
    public static final FieldTypes.Type STREAMS_TYPE = createType("streams", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type INPUT_TYPE = createType("input", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type NODE_TYPE = createType("node", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type EVENT_DEFINITION_ID_TYPE = createType("event-definition-id", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type ASSOCIATED_ASSETS_TYPE = createType("associated-assets", of(PROP_ENUMERABLE));
    public static final FieldTypes.Type PRIORITY_TYPE = createType("priority", of(PROP_ENUMERABLE));


    /**
     * A map from Elasticsearch types to Graylog logical types.
     */
    public static final Map<String, FieldTypes.Type> TYPE_MAP = ImmutableMap.<String, FieldTypes.Type>builder()
            .put("keyword", STRING_TYPE) // since ES 5.x
            .put("text", STRING_FTS_TYPE) // since ES 5.x
            .put("long", LONG_TYPE)
            .put("integer", INT_TYPE)
            .put("short", SHORT_TYPE)
            .put("byte", BYTE_TYPE)
            .put("double", DOUBLE_TYPE)
            .put("float", FLOAT_TYPE)
            .put("half_float", FLOAT_TYPE)
            .put("scaled_float", FLOAT_TYPE)
            .put("date", DATE_TYPE)
            .put("boolean", BOOLEAN_TYPE)
            .put("binary", BINARY_TYPE)
            .put("geo_point", GEO_POINT_TYPE)
            .put("ip", IP_TYPE)
            .build();

    /**
     * Checks if the given physical (Elasticsearch/OpenSearch) field type maps to a numeric Graylog type.
     *
     * @param physicalType Elasticsearch type name
     * @return true if the type is numeric
     */
    public static boolean isNumericType(String physicalType) {
        return Optional.ofNullable(TYPE_MAP.get(physicalType))
                .map(type -> type.properties().contains(PROP_NUMERIC))
                .orElse(false);
    }

    /**
     * Map the given Elasticsearch field type to a Graylog type.
     *
     * @param type Elasticsearch type name
     * @return the Graylog type object
     */
    public Optional<FieldTypes.Type> mapType(FieldTypeDTO type) {
        return Optional.ofNullable(mapFieldNameType(type))
                .or(() -> Optional.ofNullable(TYPE_MAP.get(type.physicalType())))
                .map(mappedType -> type.properties().contains(FieldTypeDTO.Properties.FIELDDATA)
                        ? mappedType.toBuilder().properties(new ImmutableSet.Builder<String>().addAll(mappedType.properties()).add(PROP_ENUMERABLE).build()).build()
                        : mappedType);
    }

    /**
     * Maps a field to a Graylog type by its (Graylog-reserved) field name, regardless of its physical type.
     */
    private static FieldTypes.Type mapFieldNameType(FieldTypeDTO type) {
        return switch (type.fieldName()) {
            case Message.FIELD_STREAMS, Message.FIELD_FAILED_MESSAGE_STREAMS -> STREAMS_TYPE;
            case Message.FIELD_GL2_SOURCE_INPUT -> INPUT_TYPE;
            case Message.FIELD_GL2_SOURCE_NODE -> NODE_TYPE;
            case EventDto.FIELD_EVENT_DEFINITION_ID -> EVENT_DEFINITION_ID_TYPE;
            case "associated_assets" -> ASSOCIATED_ASSETS_TYPE;
            case CommonEventSummary.FIELD_PRIORITY -> mapPriorityType(type);
            default -> null;
        };
    }

    /**
     * Unlike the other cases in {@link #mapFieldNameType}, "priority" is not a Graylog-reserved field name: user
     * messages can freely contain their own unrelated "priority" field. It is only mapped to
     * {@link #PRIORITY_TYPE} when it is confined to the built-in events streams, so that an unrelated message
     * field of the same name keeps its plain numeric type. This requires stream-aware field types
     * ({@code stream_aware_field_types}) to be enabled; otherwise {@link FieldTypeDTO#streams()} is always
     * empty and the field falls through to the generic {@link #TYPE_MAP} lookup.
     */
    private static FieldTypes.Type mapPriorityType(FieldTypeDTO type) {
        if (!type.streams().isEmpty() && Stream.DEFAULT_EVENT_STREAM_IDS.containsAll(type.streams())) {
            return PRIORITY_TYPE;
        }

        return null;
    }
}
