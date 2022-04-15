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
import org.graylog.plugins.views.search.elasticsearch.QueryParam;
import org.graylog.plugins.views.search.errors.EmptyParameterError;
import org.graylog.plugins.views.search.errors.MissingEnterpriseLicenseException;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.errors.UnboundParameterError;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.validators.ValidatorException;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class QueryValidationServiceImpl implements QueryValidationService {

    private final LuceneQueryParser luceneQueryParser;
    private final MappedFieldTypesService mappedFieldTypesService;
    private final Set<QueryValidator> validators;

    @Inject
    public QueryValidationServiceImpl(LuceneQueryParser luceneQueryParser,
                                      MappedFieldTypesService mappedFieldTypesService,
                                      Set<QueryValidator> validators) {
        this.luceneQueryParser = luceneQueryParser;
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.validators = validators;
    }

    @Override
    public ValidationResponse validate(ValidationRequest req) {
        // caution, there are two validation steps!
        // the validation uses query with _non_replaced parameters, as is, to be able to track the exact positions of errors
        final String rawQuery = req.query().queryString();

        if (StringUtils.isEmpty(rawQuery)) {
            return ValidationResponse.ok();
        }

        try {
            final ParsedQuery parsedQuery = luceneQueryParser.parse(rawQuery);
            Set<MappedFieldTypeDTO> availableFields = mappedFieldTypesService.fieldTypesByStreamIds(req.streams(), req.timerange());

            final ValidationContext context = ValidationContext.builder()
                    .request(req)
                    .query(parsedQuery)
                    .availableFields(availableFields)
                    .build();

            final List<ValidationMessage> explanations = validators.stream()
                    .flatMap(val -> val.validate(context).stream())
                    .collect(Collectors.toList());

            return explanations.isEmpty()
                    ? ValidationResponse.ok()
                    : ValidationResponse.warning(explanations);

        } catch (ParseException | ValidatorException e) {
            return ValidationResponse.error(toExplanation(e));
        } catch (SearchException searchException) {
            return convert(searchException);
        } catch (MissingEnterpriseLicenseException licenseException) {
            return ValidationResponse.error(
                    paramsToValidationMessages(
                            licenseException.getQueryParams(),
                            ValidationType.MISSING_LICENSE,
                            param -> "Search parameter used without enterprise license: " + param.name()
                    ));
        }
    }

    private ValidationResponse convert(SearchException searchException) {
        if (searchException.error() instanceof UnboundParameterError) {
            final UnboundParameterError error = (UnboundParameterError) searchException.error();
            return ValidationResponse.error(paramsToValidationMessages(
                    error.allUnknownParameters(),
                    ValidationType.UNDECLARED_PARAMETER,
                    param -> "Unbound required parameter used: " + param.name()
            ));
        } else if (searchException.error() instanceof EmptyParameterError) {
            final EmptyParameterError error = (EmptyParameterError) searchException.error();
            return ValidationResponse.warning(paramsToValidationMessages(
                    Collections.singleton(error.getParameterUsage()),
                    ValidationType.EMPTY_PARAMETER,
                    param -> error.description()));
        }
        return ValidationResponse.error(Collections.singletonList(ValidationMessage.fromException(searchException)));
    }

    private List<ValidationMessage> paramsToValidationMessages(final Set<QueryParam> params, final ValidationType errorType, final Function<QueryParam, String> messageBuilder) {
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

    private List<ValidationMessage> toExplanation(final Exception parseException) {
        return Collections.singletonList(ValidationMessage.fromException(parseException));
    }
}
