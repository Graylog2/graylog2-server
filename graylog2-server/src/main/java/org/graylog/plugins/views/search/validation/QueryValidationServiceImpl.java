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

import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.elasticsearch.QueryParam;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.errors.EmptyParameterError;
import org.graylog.plugins.views.search.errors.MissingEnterpriseLicenseException;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.errors.UnboundParameterError;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.fields.UnknownFieldsIdentifier;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class QueryValidationServiceImpl implements QueryValidationService {

    private final LuceneQueryParser luceneQueryParser;
    private final MappedFieldTypesService mappedFieldTypesService;
    private final QueryStringDecorators queryStringDecorators;
    private final FieldTypeValidation fieldTypeValidation;
    private final QueryValidator unknownFieldsIdentifier;

    @Inject
    public QueryValidationServiceImpl(LuceneQueryParser luceneQueryParser,
                                      MappedFieldTypesService mappedFieldTypesService,
                                      QueryStringDecorators queryStringDecorators,
                                      FieldTypeValidation fieldTypeValidation,
                                      UnknownFieldsIdentifier unknownFieldsIdentifier) {
        this.luceneQueryParser = luceneQueryParser;
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.queryStringDecorators = queryStringDecorators;
        this.fieldTypeValidation = fieldTypeValidation;
        this.unknownFieldsIdentifier = unknownFieldsIdentifier;
    }

    @Override
    public ValidationResponse validate(ValidationRequest req) {
        // caution, there are two validation steps!
        // the validation uses query with _non_replaced parameters, as is, to be able to track the exact positions of errors
        final String rawQuery = req.query().queryString();

        if (StringUtils.isEmpty(rawQuery)) {
            return ValidationResponse.ok();
        }

        String decorated;
        try {
            decorated = decoratedQuery(req);
        } catch (SearchException searchException) {
            return convert(searchException);
        } catch (MissingEnterpriseLicenseException licenseException) {
            return ValidationResponse.error(
                    paramsToValidationErrors(
                            licenseException.getQueryParams(),
                            ValidationType.MISSING_LICENSE,
                            param -> "Search parameter used without enterprise license: " + param.name()
                    ));
        }

        try {
            final ParsedQuery parsedQuery = luceneQueryParser.parse(rawQuery);
            Set<MappedFieldTypeDTO> availableFields = mappedFieldTypesService.fieldTypesByStreamIds(req.streams(), req.timerange());
            final List<ValidationMessage> explanations = new ArrayList<>();

            explanations.addAll(unknownFieldsIdentifier.validate(req, parsedQuery));
            explanations.addAll(invalidOperators(parsedQuery.invalidOperators()));
            explanations.addAll(validateQueryValues(rawQuery, decorated, availableFields));

            return explanations.isEmpty()
                    ? ValidationResponse.ok()
                    : ValidationResponse.warning(explanations);

        } catch (ParseException e) {
            return ValidationResponse.error(toExplanation(e));
        }
    }

    private List<ValidationMessage> invalidOperators(List<ImmutableToken> invalidOperators) {
        return invalidOperators.stream().map(token -> {
            final String errorMessage = String.format(Locale.ROOT, "Query contains invalid operator \"%s\". All AND / OR / NOT operators have to be written uppercase", token.image());
            return ValidationMessage.builder(ValidationType.INVALID_OPERATOR)
                    .errorMessage(errorMessage)
                    .beginLine(token.beginLine())
                    .beginColumn(token.beginColumn())
                    .endLine(token.endLine())
                    .endColumn(token.endColumn())
                    .build();
        }).collect(Collectors.toList());
    }

    private ValidationResponse convert(SearchException searchException) {
        if (searchException.error() instanceof UnboundParameterError) {
            final UnboundParameterError error = (UnboundParameterError) searchException.error();
            return ValidationResponse.error(paramsToValidationErrors(
                    error.allUnknownParameters(),
                    ValidationType.UNDECLARED_PARAMETER,
                    param -> "Unbound required parameter used: " + param.name()
            ));
        } else if (searchException.error() instanceof EmptyParameterError) {
            final EmptyParameterError error = (EmptyParameterError) searchException.error();
            return ValidationResponse.warning(paramsToValidationErrors(
                    Collections.singleton(error.getParameterUsage()),
                    ValidationType.EMPTY_PARAMETER,
                    param -> error.description()));
        }
        return ValidationResponse.error(Collections.singletonList(ValidationMessage.fromException(searchException)));
    }

    private List<ValidationMessage> paramsToValidationErrors(final Set<QueryParam> params, final ValidationType errorType, final Function<QueryParam, String> messageBuilder) {
        return params.stream()
                .flatMap(param -> {
                    final String errorMessage = messageBuilder.apply(param);
                    return param.positions()
                            .stream()
                            .map(p -> ValidationMessage.builder(errorType)
                                    .errorMessage(errorMessage)
                                    .beginLine(p.line())
                                    .endLine(p.line())
                                    .beginColumn(p.beginColumn())
                                    .endColumn(p.endColumn())
                                    .relatedProperty(param.name())
                                    .build()
                            );
                })
                .collect(Collectors.toList());
    }

    private List<ValidationMessage> toExplanation(final ParseException parseException) {
        return Collections.singletonList(ValidationMessage.fromException(parseException));
    }

    private List<ValidationMessage> validateQueryValues(String rawQuery, String decorated, Set<MappedFieldTypeDTO> availableFields) throws ParseException {
        final ParsedQuery parsedQuery = luceneQueryParser.parse(decorated);
        final Map<String, MappedFieldTypeDTO> fields = availableFields.stream().collect(Collectors.toMap(MappedFieldTypeDTO::name, Function.identity()));

        return parsedQuery.terms().stream()
                .map(term -> {
                    final MappedFieldTypeDTO fieldType = fields.get(term.getRealFieldName());
                    final Optional<String> typeName = Optional.ofNullable(fieldType)
                            .map(MappedFieldTypeDTO::type)
                            .map(FieldTypes.Type::type);
                    return typeName.flatMap(type -> fieldTypeValidation.validateFieldValueType(term, type));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private String decoratedQuery(ValidationRequest req) {
        ParameterProvider parameterProvider = (name) -> req.parameters().stream().filter(p -> Objects.equals(p.name(), name)).findFirst();
        final Query query = Query.builder().query(req.query()).timerange(req.timerange()).build();
        return this.queryStringDecorators.decorate(req.getCombinedQueryWithFilter(), parameterProvider, query);
    }
}
