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
package org.graylog.plugins.views.search.validation.validators;

import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.validation.ValidationContext;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog.plugins.views.search.validation.validators.util.UnknownFieldsListLimiter;

import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.RESERVED_SETTABLE_FIELDS;
import static org.graylog2.plugin.Message.SEARCHABLE_ES_FIELDS;

@Singleton
public class UnknownFieldsValidator implements QueryValidator {

    private final UnknownFieldsListLimiter unknownFieldsListLimiter = new UnknownFieldsListLimiter();


    @Override
    public List<ValidationMessage> validate(final ValidationContext context) {
        final Set<String> availableFields = context.availableFields()
                .stream()
                .map(MappedFieldTypeDTO::name)
                .collect(Collectors.toSet());
        final List<ParsedTerm> terms = context.query().terms();

        return identifyUnknownFields(availableFields, terms).stream().map(field -> {
            final ValidationMessage.Builder message = ValidationMessage.builder(ValidationStatus.WARNING, ValidationType.UNKNOWN_FIELD)
                    .relatedProperty(field.getRealFieldName())
                    .errorMessage("Query contains unknown field: " + field.getRealFieldName());

            field.keyToken()
                    .map(QueryPosition::from)
                    .ifPresent(message::position);

            return message.build();
        }).collect(Collectors.toList());
    }

    List<ParsedTerm> identifyUnknownFields(final Set<String> availableFields, final List<ParsedTerm> terms) {
        final Map<String, List<ParsedTerm>> groupedByField = terms.stream()
                .filter(t -> !t.isDefaultField())
                .filter(term -> !SEARCHABLE_ES_FIELDS.contains(term.getRealFieldName()))
                .filter(term -> !RESERVED_SETTABLE_FIELDS.contains(term.getRealFieldName()))
                .filter(term -> !availableFields.contains(term.getRealFieldName()))
                .distinct()
                .collect(Collectors.groupingBy(ParsedTerm::getRealFieldName));

        return unknownFieldsListLimiter.filterElementsContainingUsefulInformation(groupedByField);
    }
}
