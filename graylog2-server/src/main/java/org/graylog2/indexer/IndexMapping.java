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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.TemplateIndexSetConfig;
import org.graylog2.indexer.indices.Template;
import org.graylog2.plugin.Message;

import java.util.List;
import java.util.Map;

import static org.graylog2.plugin.Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS;

/**
 * Representing the message type mapping in Elasticsearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 */
public abstract class IndexMapping implements IndexMappingTemplate {
    public static final String TYPE_MESSAGE = "message";

    @Override
    public Template toTemplate(final TemplateIndexSetConfig indexSetConfig,
                               final Long order) {
        return messageTemplate(indexSetConfig.indexWildcard(),
                indexSetConfig.indexAnalyzer(),
                order,
                indexSetConfig.customFieldMappings());
    }

    protected Map<String, Object> analyzerKeyword() {
        return ImmutableMap.of("analyzer_keyword", ImmutableMap.of(
                "tokenizer", "keyword",
                "filter", "lowercase"));
    }

    public Template messageTemplate(final String indexPattern,
                                               final String analyzer,
                                               final Long order,
                                               final CustomFieldMappings customFieldMappings) {
        var settings = new Template.Settings(Map.of(
                "index", Map.of(
                        "analysis", Map.of("analyzer", analyzerKeyword())
        )
        ));
        var mappings = mapping(analyzer, customFieldMappings);

        return createTemplate(indexPattern, order, settings, mappings);
    }

    Template createTemplate(String indexPattern, Long order, Template.Settings settings, Template.Mappings mappings) {
        return Template.create(indexPattern, mappings, order, settings);
    }

    protected Template.Mappings mapping(final String analyzer,
                                          final CustomFieldMappings customFieldMappings) {
        return new Template.Mappings(ImmutableMap.of(TYPE_MESSAGE, messageMapping(analyzer, customFieldMappings)));
    }

    protected Map<String, Object> messageMapping(final String analyzer,
                                                 final CustomFieldMappings customFieldMappings) {
        return ImmutableMap.of(
                "properties", fieldProperties(analyzer, customFieldMappings),
                "dynamic_templates", dynamicTemplate(),
                "_source", enabled());
    }

    private Map<String, Map<String, Object>> internalFieldsMapping() {
        return ImmutableMap.of("internal_fields",
                ImmutableMap.of(
                        "match", "gl2_*",
                        "match_mapping_type", "string",
                        "mapping", notAnalyzedString())
        );
    }

    protected List<Map<String, Map<String, Object>>> dynamicTemplate() {
        final Map<String, Map<String, Object>> templateInternal = internalFieldsMapping();

        final Map<String, Map<String, Object>> templateAll = ImmutableMap.of("store_generic", dynamicStrings());

        return ImmutableList.of(templateInternal, templateAll);
    }

    abstract Map<String, Object> dynamicStrings();

    protected Map<String, Map<String, Object>> fieldProperties(final String analyzer,
                                                               final CustomFieldMappings customFieldMappings) {
        final ImmutableMap.Builder<String, Map<String, Object>> builder = ImmutableMap.<String, Map<String, Object>>builder()
                .put(Message.FIELD_MESSAGE, analyzedString(analyzer, false))
                // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
                // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
                .put(Message.FIELD_TIMESTAMP, typeTimeWithMillis())
                .put(Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, typeLong())
                .put(Message.FIELD_GL2_RECEIVE_TIMESTAMP, typeTimeWithMillis())
                .put(Message.FIELD_GL2_PROCESSING_TIMESTAMP, typeTimeWithMillis())
                .put(Message.FIELD_GL2_MESSAGE_ID, notAnalyzedString())
                .put(Message.FIELD_STREAMS, notAnalyzedString())
                // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
                .put(Message.FIELD_SOURCE, analyzedString("analyzer_keyword", true));


        if (customFieldMappings != null) {
            customFieldMappings.stream()
                    .filter(customMapping -> !FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS.contains(customMapping.fieldName())) //someone might have hardcoded reserved field mapping on MongoDB level, bypassing checks
                    .forEach(customMapping -> builder.put(customMapping.fieldName(), type(customMapping.toPhysicalType())));
        }

        //those FIELD_FULL_MESSAGE field have not been yet made reserved, so it can be added to ImmutableMap only if they do not exist in Custom Mapping
        if (customFieldMappings == null || !customFieldMappings.containsCustomMappingForField(Message.FIELD_FULL_MESSAGE)) {
            builder.put(Message.FIELD_FULL_MESSAGE, analyzedString(analyzer, false));
        }

        return builder.build();
    }

    Map<String, Object> notAnalyzedString() {
        return ImmutableMap.of("type", "keyword");
    }
    Map<String, Object> analyzedString(String analyzer, boolean fieldData) {
        return ImmutableMap.of(
                "type", "text",
                "analyzer", analyzer,
                "fielddata", fieldData);
    }

    protected Map<String, Object> typeTimeWithMillis() {
        return ImmutableMap.of(
                "type", "date",
                "format", dateFormat());
    }

    protected Map<String, Object> typeLong() {
        return ImmutableMap.of("type", "long");
    }

    protected Map<String, Object> type(final String type) {
        return ImmutableMap.of("type", type);
    }

    private Map<String, Boolean> enabled() {
        return ImmutableMap.of("enabled", true);
    }

    protected String dateFormat() {
        return Constants.ES_DATE_FORMAT;
    }
}
