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
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class UnknownFieldsIdentifier {

    private final MappedFieldTypesService mappedFieldTypesService;

    @Inject
    public UnknownFieldsIdentifier(final MappedFieldTypesService mappedFieldTypesService) {
        this.mappedFieldTypesService = mappedFieldTypesService;
    }

    public List<ParsedTerm> identifyUnknownFields(final ValidationRequest req, final Collection<ParsedTerm> parsedQueryTerms) {
        if (req == null || parsedQueryTerms == null) {
            return Collections.emptyList();
        }
        final Set<String> availableFields = mappedFieldTypesService.fieldTypesByStreamIds(req.streams(), req.timerange())
                .stream()
                .map(MappedFieldTypeDTO::name)
                .collect(Collectors.toSet());

        return parsedQueryTerms.stream()
                .filter(t -> !t.isDefaultField())
                .filter(term -> !availableFields.contains(term.getRealFieldName()))
                .collect(Collectors.toList());
    }
}
