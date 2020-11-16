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
package org.graylog2.decorators;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class LookupTableDecorator implements SearchResponseDecorator {
    private static final String CK_SOURCE_FIELD = "source_field";
    private static final String CK_TARGET_FIELD = "target_field";
    private static final String CK_LOOKUP_TABLE_NAME = "lookup_table_name";

    private final String sourceField;
    private final String targetField;
    private final LookupTableService.Function lookupTable;

    public interface Factory extends SearchResponseDecorator.Factory {
        @Override
        LookupTableDecorator create(Decorator decorator);

        @Override
        LookupTableDecorator.Config getConfig();

        @Override
        LookupTableDecorator.Descriptor getDescriptor();
    }

    public static class Config implements SearchResponseDecorator.Config {
        private final DBLookupTableService lookupTableService;

        @Inject
        public Config(DBLookupTableService lookupTableService) {
            this.lookupTableService = lookupTableService;
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final Map<String, String> lookupTables = lookupTableService.findAll().stream()
                    .collect(Collectors.toMap(LookupTableDto::name, LookupTableDto::title));

            return new ConfigurationRequest() {
                {
                    addField(new TextField(
                            CK_SOURCE_FIELD,
                            "Source field",
                            "",
                            "The message field which includes the value to look up.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    ));
                    addField(new TextField(
                            CK_TARGET_FIELD,
                            "Target field",
                            "",
                            "The message field that will be created with the result of the lookup.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    ));
                    addField(new DropdownField(
                            CK_LOOKUP_TABLE_NAME,
                            "Lookup table",
                            "",
                            lookupTables,
                            "The lookup table to use.",
                            ConfigurationField.Optional.NOT_OPTIONAL
                    ));
                }
            };
        }
    }

    public static class Descriptor extends SearchResponseDecorator.Descriptor {
        public Descriptor() {
            super("Lookup Table", "http://docs.graylog.org/", "Lookup Table Decorator");
        }
    }

    @Inject
    public LookupTableDecorator(@Assisted Decorator decorator, LookupTableService lookupTableService) {
        final String sourceField = (String) decorator.config().get(CK_SOURCE_FIELD);
        final String targetField = (String) decorator.config().get(CK_TARGET_FIELD);
        final String lookupTableName = (String) decorator.config().get(CK_LOOKUP_TABLE_NAME);

        if (isNullOrEmpty(sourceField)) {
            throw new IllegalStateException("Missing configuration field: " + CK_SOURCE_FIELD);
        }
        if (isNullOrEmpty(targetField)) {
            throw new IllegalStateException("Missing configuration field: " + CK_TARGET_FIELD);
        }
        if (isNullOrEmpty(lookupTableName)) {
            throw new IllegalStateException("Missing configuration field: " + CK_LOOKUP_TABLE_NAME);
        }

        if (!lookupTableService.hasTable(lookupTableName)) {
            throw new IllegalStateException("Configured lookup table <" + lookupTableName + "> doesn't exist");
        }

        this.sourceField = sourceField;
        this.targetField = targetField;
        this.lookupTable = lookupTableService.newBuilder().lookupTable(lookupTableName).build();
    }

    @Override
    public SearchResponse apply(SearchResponse searchResponse) {
        final List<ResultMessageSummary> summaries = searchResponse.messages().stream()
                .map(summary -> {
                    // Do not touch the message if the field does not exist.
                    if (!summary.message().containsKey(sourceField)) {
                        return summary;
                    }

                    final LookupResult result = lookupTable.lookup(summary.message().get(sourceField));

                    // Do not touch the message if there is no result
                    if (result == null || result.isEmpty()) {
                        return summary;
                    }

                    final Message message = new Message(ImmutableMap.copyOf(summary.message()));

                    message.addField(targetField, result.singleValue());

                    return summary.toBuilder().message(message.getFields()).build();
                })
                .collect(Collectors.toList());

        return searchResponse.toBuilder().messages(summaries).build();
    }
}
