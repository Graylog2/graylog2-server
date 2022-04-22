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

import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.engine.PositionTrackingQuery;
import org.graylog.plugins.views.search.errors.MissingEnterpriseLicenseException;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.FieldTypeValidation;
import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.validation.ValidationContext;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog2.indexer.fieldtypes.FieldTypes;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FieldValueTypeValidator implements QueryValidator {

    private final FieldTypeValidation fieldTypeValidation;
    private final LuceneQueryParser luceneQueryParser;
    private final QueryStringDecorators queryStringDecorators;

    @Inject
    public FieldValueTypeValidator(FieldTypeValidation fieldTypeValidation, LuceneQueryParser luceneQueryParser, QueryStringDecorators queryStringDecorators) {
        this.fieldTypeValidation = fieldTypeValidation;
        this.luceneQueryParser = luceneQueryParser;
        this.queryStringDecorators = queryStringDecorators;
    }

    @Override
    public List<ValidationMessage> validate(ValidationContext context) {
        try {
            PositionTrackingQuery decorated = decoratedQuery(context.request());
            return validateQueryValues(decorated, context.availableFields());
        } catch (ParseException e) {
            return ValidationErrors.create(e);
        } catch (SearchException e) {
            return ValidationErrors.create(e);
        } catch (MissingEnterpriseLicenseException e) {
            return ValidationErrors.create(e);

        }
    }

    private PositionTrackingQuery decoratedQuery(ValidationRequest req) {
        ParameterProvider parameterProvider = (name) -> req.parameters().stream().filter(p -> Objects.equals(p.name(), name)).findFirst();
        final Query query = Query.builder().query(req.query()).timerange(req.timerange()).build();
        return this.queryStringDecorators.decorateWithPositions(req.getCombinedQueryWithFilter(), parameterProvider, query);
    }

    private List<ValidationMessage> validateQueryValues(PositionTrackingQuery decorated, Set<MappedFieldTypeDTO> availableFields) throws ParseException {
        final ParsedQuery parsedQuery = luceneQueryParser.parse(decorated.getInterpolatedQuery());
        final Map<String, MappedFieldTypeDTO> fields = availableFields.stream().collect(Collectors.toMap(MappedFieldTypeDTO::name, Function.identity()));

        return parsedQuery.terms().stream()
                .map(term -> {
                    final MappedFieldTypeDTO fieldType = fields.get(term.getRealFieldName());
                    final Optional<String> typeName = Optional.ofNullable(fieldType)
                            .map(MappedFieldTypeDTO::type)
                            .map(FieldTypes.Type::type);
                    return typeName.flatMap(type -> fieldTypeValidation.validateFieldValueType(term, type))
                            .map(validation -> validation.toBuilder()
                                    .position(decorated.backtrackPosition(validation.position()))
                                    .build());
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
