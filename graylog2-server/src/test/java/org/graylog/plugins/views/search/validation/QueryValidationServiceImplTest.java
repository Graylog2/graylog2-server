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
        final MappedFieldTypesService fields = (streamIds, timeRange) -> Collections.emptySet();

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
        final MappedFieldTypesService fields = (streamIds, timeRange) -> Collections.emptySet();

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
        final MappedFieldTypesService fields = (streamIds, timeRange) -> Collections.emptySet();

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
