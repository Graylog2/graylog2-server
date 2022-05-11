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

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class QueryValidationServiceImplTest {

    public static final MappedFieldTypesService FIELD_TYPES_SERVICE = (streamIds, timeRange) -> Collections.emptySet();
    public static final LuceneQueryParser LUCENE_QUERY_PARSER = new LuceneQueryParser();

    @Test
    void validateNoMessages() {
         // validator doesn't return any warnings or errors
        final QueryValidator queryValidator = context -> Collections.emptyList();

        final QueryValidationServiceImpl service = new QueryValidationServiceImpl(
                LUCENE_QUERY_PARSER,
                FIELD_TYPES_SERVICE,
                Collections.singleton(queryValidator));

        final ValidationResponse validationResponse = service.validate(req());

        assertThat(validationResponse.status()).isEqualTo(ValidationStatus.OK);
        assertThat(validationResponse.explanations()).isEmpty();
    }

    @Test
    void validateWithWarning() {
        // validator returns one warning
        final QueryValidator queryValidator = context -> Collections.singletonList(
                ValidationMessage.builder(ValidationStatus.WARNING, ValidationType.INVALID_OPERATOR)
                        .errorMessage("Invalid operator detected")
                        .build());

        final QueryValidationServiceImpl service = new QueryValidationServiceImpl(
                LUCENE_QUERY_PARSER,
                FIELD_TYPES_SERVICE,
                Collections.singleton(queryValidator));

        final ValidationResponse validationResponse = service.validate(req());

        assertThat(validationResponse.status()).isEqualTo(ValidationStatus.WARNING);
        assertThat(validationResponse.explanations())
                .hasOnlyOneElementSatisfying(message -> {
                    assertThat(message.validationType()).isEqualTo(ValidationType.INVALID_OPERATOR);
                    assertThat(message.validationStatus()).isEqualTo(ValidationStatus.WARNING);
                });
    }

    @Test
    void validateWithError() {
        // validator returns one warning
        final QueryValidator queryValidator = context -> Collections.singletonList(
                ValidationMessage.builder(ValidationStatus.ERROR, ValidationType.QUERY_PARSING_ERROR)
                        .errorMessage("Query can't be parsed")
                        .build());

        final QueryValidationServiceImpl service = new QueryValidationServiceImpl(
                LUCENE_QUERY_PARSER,
                FIELD_TYPES_SERVICE,
                Collections.singleton(queryValidator));

        final ValidationResponse validationResponse = service.validate(req());

        assertThat(validationResponse.status()).isEqualTo(ValidationStatus.ERROR);
        assertThat(validationResponse.explanations())
                .hasOnlyOneElementSatisfying(message -> {
                    assertThat(message.validationType()).isEqualTo(ValidationType.QUERY_PARSING_ERROR);
                    assertThat(message.validationStatus()).isEqualTo(ValidationStatus.ERROR);
                });
    }


    @Test
    void validateMixedTypes() {

        // validator returns one error
        final QueryValidator errorValidator = context -> Collections.singletonList(
                ValidationMessage.builder(ValidationStatus.ERROR, ValidationType.QUERY_PARSING_ERROR)
                        .errorMessage("Query can't be parsed")
                        .build());


        // validator returns one warning
        final QueryValidator warningValidator = context -> Collections.singletonList(
                ValidationMessage.builder(ValidationStatus.WARNING, ValidationType.UNKNOWN_FIELD)
                        .errorMessage("Unknown field")
                        .build());

        final QueryValidationServiceImpl service = new QueryValidationServiceImpl(
                new LuceneQueryParser(),
                FIELD_TYPES_SERVICE,
                ImmutableSet.of(warningValidator, errorValidator));

        final ValidationResponse validationResponse = service.validate(req());

        assertThat(validationResponse.status()).isEqualTo(ValidationStatus.ERROR);
        assertThat(validationResponse.explanations())
                .hasSize(2)
                .extracting(ValidationMessage::validationStatus)
                .containsOnly(ValidationStatus.ERROR, ValidationStatus.WARNING);
    }

    private ValidationRequest req() {
        return ValidationRequest.builder()
                .query(ElasticsearchQueryString.of("foo:bar"))
                .streams(Collections.emptySet())
                .timerange(RelativeRange.create(300))
                .build();
    }
}
