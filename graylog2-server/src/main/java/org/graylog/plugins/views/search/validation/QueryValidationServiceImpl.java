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
package org.graylog.plugins.views.search.validation;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class QueryValidationServiceImpl implements QueryValidationService {

    private final LuceneQueryParser luceneQueryParser;
    private final MappedFieldTypesService mappedFieldTypesService;
    private final QueryStringDecorators queryStringDecorators;

    @Inject
    public QueryValidationServiceImpl(LuceneQueryParser luceneQueryParser, MappedFieldTypesService mappedFieldTypesService, QueryStringDecorators queryStringDecorators) {
        this.luceneQueryParser = luceneQueryParser;
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.queryStringDecorators = queryStringDecorators;
    }

    @Override
    public ValidationResponse validate(ValidationRequest req) {
        final String query = decoratedQuery(req);

        if(StringUtils.isEmpty(query)) {
            return ValidationResponse.ok();
        }

        try {
            final ParsedQuery parsedQuery = luceneQueryParser.parse(query);
            final List<ParsedTerm> unknownFields = getUnknownFields(req, parsedQuery);
            final List<ParsedTerm> invalidOperators = parsedQuery.invalidOperators();
            final List<ValidationMessage> explanations = getExplanations(unknownFields, invalidOperators);

            return explanations.isEmpty()
                    ? ValidationResponse.ok()
                    : ValidationResponse.warning(explanations);

        } catch (ParseException e) {
            return ValidationResponse.error(toExplanation(query, e));
        }
    }

    private List<ValidationMessage> toExplanation(final String query, final ParseException parseException) {
        return Collections.singletonList(ValidationMessage.fromException(query, parseException));
    }

    private List<ValidationMessage> getExplanations(List<ParsedTerm> unknownFields, List<ParsedTerm> invalidOperators) {
        List<ValidationMessage> messages = new ArrayList<>();

        unknownFields.stream().map(f -> {
            final ValidationMessage.Builder message = ValidationMessage.builder()
                    .errorType("Unknown field")
                    .errorMessage("Query contains unknown field: " + f.getRealFieldName());

            f.tokens().stream().findFirst().ifPresent(t -> {
                message.beginLine(t.beginLine());
                message.beginColumn(t.beginColumn());
                message.endLine(t.endLine());
                message.endColumn(t.endColumn());
            });

            return message.build();

        }).forEach(messages::add);

        invalidOperators.stream()
                .map(token -> {
                    final String errorMessage = String.format(Locale.ROOT, "Query contains invalid operator \"%s\". Both AND / OR operators have to be written uppercase", token.value());
                    final ValidationMessage.Builder message = ValidationMessage.builder()
                            .errorType("Invalid operator")
                            .errorMessage(errorMessage);
                    token.tokens().stream().findFirst().ifPresent(t -> {
                        message.beginLine(t.beginLine());
                        message.beginColumn(t.beginColumn());
                        message.endLine(t.endLine());
                        message.endColumn(t.endColumn());
                    });
                    return message.build();
                })
                .forEach(messages::add);
        return messages;
    }

    private List<ParsedTerm> getUnknownFields(ValidationRequest req, ParsedQuery query) {
        final Set<String> availableFields = mappedFieldTypesService.fieldTypesByStreamIds(req.streams(), req.timerange())
                .stream()
                .map(MappedFieldTypeDTO::name)
                .collect(Collectors.toSet());

        return query.terms().stream()
                .filter(t -> !t.isDefaultField())
                .filter(term -> !availableFields.contains(term.getRealFieldName()))
                .collect(Collectors.toList());
    }

    private String decoratedQuery(ValidationRequest req) {
        ParameterProvider parameterProvider = (name) -> req.parameters().stream().filter(p -> Objects.equals(p.name(), name)).findFirst();
        final Query query = Query.builder().query(req.query()).timerange(req.timerange()).build();
        return this.queryStringDecorators.decorate(req.getCombinedQueryWithFilter(), parameterProvider, query);
    }
}
