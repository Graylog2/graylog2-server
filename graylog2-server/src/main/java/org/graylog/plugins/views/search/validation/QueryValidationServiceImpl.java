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
        try {
            final ParsedQuery parsedQuery = luceneQueryParser.parse(query);
            final List<ParsedTerm> unknownFields = getUnknownFields(req, parsedQuery);
            final List<ParsedTerm> unknownTokens = parsedQuery.unknownTokens();
            final List<ValidationMessage> explanations = getExplanations(unknownFields, unknownTokens);
            final ValidationStatus status = explanations.isEmpty() ? ValidationStatus.OK : ValidationStatus.WARNING;
            return ValidationResponse.builder(status)
                    .explanations(explanations)
                    .unknownFields(unknownFields.stream().map(ParsedTerm::getRealFieldName).collect(Collectors.toSet()))
                    .unknownTokens(unknownTokens.stream().map(ParsedTerm::value).collect(Collectors.toSet()))
                    .build();

        } catch (ParseException e) {
            return  ValidationResponse.builder(ValidationStatus.ERROR).explanations(toExplanation(e)).build();
        }
    }

    private List<ValidationMessage> toExplanation(ParseException parseException) {
        return Collections.singletonList(ValidationMessage.fromException(parseException));
    }

    private List<ValidationMessage> getExplanations(List<ParsedTerm> unknownFields, List<ParsedTerm> unknownTokens) {
        List<ValidationMessage> messages = new ArrayList<>();

        unknownFields.stream().map(f -> {
            final ValidationMessage.Builder message = ValidationMessage.builder()
                    .errorType("Unknown field")
                    .errorMessage("Query contains unknown field: " + f.getRealFieldName());

            f.tokens().stream().findFirst().ifPresent(t -> {
                message.beginLine(t.beginLine);
                message.beginColumn(t.beginColumn);
                message.endLine(t.endLine);
                message.endColumn(t.endColumn);
            });

            return message.build();

        }).forEach(messages::add);

        unknownTokens.stream()
                .map(token -> {
                    final ValidationMessage.Builder message = ValidationMessage.builder()
                            .errorType("Unknown token")
                            .errorMessage("Query contains unrecognized token: " + token.value());
                    token.tokens().stream().findFirst().ifPresent(t -> {
                        message.beginLine(t.beginLine);
                        message.beginColumn(t.beginColumn);
                        message.endLine(t.endLine);
                        message.endColumn(t.endColumn);
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
                .filter(t -> !t.isUnknownToken())
                .filter(term -> !availableFields.contains(term.getRealFieldName()))
                .collect(Collectors.toList());
    }

    private String decoratedQuery(ValidationRequest req) {
        ParameterProvider parameterProvider = (name) -> req.parameters().stream().filter(p -> Objects.equals(p.name(), name)).findFirst();
        final Query query = Query.builder().query(req.query()).timerange(req.timerange()).build();
        return this.queryStringDecorators.decorate(req.getCombinedQueryWithFilter(), parameterProvider, query, Collections.emptySet());
    }
}
