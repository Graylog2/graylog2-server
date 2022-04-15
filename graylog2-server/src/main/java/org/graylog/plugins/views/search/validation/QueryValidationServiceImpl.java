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

import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class QueryValidationServiceImpl implements QueryValidationService {

    private final LuceneQueryParser luceneQueryParser;
    private final MappedFieldTypesService fields;
    private final Set<QueryValidator> validators;

    @Inject
    public QueryValidationServiceImpl(LuceneQueryParser luceneQueryParser,
                                      MappedFieldTypesService fields,
                                      Set<QueryValidator> validators) {
        this.luceneQueryParser = luceneQueryParser;
        this.fields = fields;
        this.validators = validators;
    }

    @Override
    public ValidationResponse validate(ValidationRequest req) {

        if (req.isEmptyQuery()) {
            return ValidationResponse.ok();
        }

        try {
            final ParsedQuery parsedQuery = luceneQueryParser.parse(req.rawQuery());

            final ValidationContext context = ValidationContext.builder()
                    .request(req)
                    .query(parsedQuery)
                    .availableFields(fields.fieldTypesByStreamIds(req.streams(), req.timerange()))
                    .build();

            final List<ValidationMessage> explanations = validators.stream()
                    .flatMap(val -> val.validate(context).stream())
                    .collect(Collectors.toList());

            return ValidationResponse.withDetectedStatus(explanations);

        } catch (Exception e) {
            return ValidationResponse.error(ValidationMessage.fromException(e));
        }
    }
}
