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
package org.graylog.plugins.views.search.validation.validators.util;

import org.graylog.plugins.views.search.validation.ParsedTerm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnknownFieldsListLimiter {

    public List<ParsedTerm> filterElementsContainingUsefulInformation(final Map<String, List<ParsedTerm>> parsedTermsGroupedByField) {
        return parsedTermsGroupedByField.values()
                .stream()
                .map(this::filterElementsContainingUsefulInformation)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<ParsedTerm> filterElementsContainingUsefulInformation(final List<ParsedTerm> fieldTerms) {
        if (fieldTerms.size() < 2) {
            return fieldTerms;
        } else {
            if (fieldTerms.stream().anyMatch(parsedTerm -> parsedTerm.keyToken().isPresent())) {
                //there is positional info - return only ParsedTerm instances with position
                return fieldTerms.stream().filter(parsedTerm -> parsedTerm.keyToken().isPresent()).toList();
            } else {
                //no positional info - it is enough to return a single ParsedTerm instance
                return List.of(fieldTerms.get(0));
            }
        }
    }
}
