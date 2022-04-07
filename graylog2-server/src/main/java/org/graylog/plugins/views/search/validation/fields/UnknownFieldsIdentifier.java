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
package org.graylog.plugins.views.search.validation.fields;

import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.validation.ValidationContext;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class UnknownFieldsIdentifier implements QueryValidator {

    @Inject
    public UnknownFieldsIdentifier() {
    }

    @Override
    public List<ValidationMessage> validate(ValidationContext context) {
        return identifyUnknownFields(context).stream().map(f -> {
            final ValidationMessage.Builder message = ValidationMessage.builder(ValidationType.UNKNOWN_FIELD)
                    .relatedProperty(f.getRealFieldName())
                    .errorMessage("Query contains unknown field: " + f.getRealFieldName());
            f.keyToken().ifPresent(t -> {
                message.beginLine(t.beginLine());
                message.beginColumn(t.beginColumn());
                message.endLine(t.endLine());
                message.endColumn(t.endColumn());
            });
            return message.build();
        }).collect(Collectors.toList());
    }

    private List<ParsedTerm> identifyUnknownFields(final ValidationContext context) {
        final Set<String> availableFields = context.availableFields()
                .stream()
                .map(MappedFieldTypeDTO::name)
                .collect(Collectors.toSet());

        return context.query().terms().stream()
                .filter(t -> !t.isDefaultField())
                .filter(term -> !availableFields.contains(term.getRealFieldName()))
                .collect(Collectors.toList());
    }
}
