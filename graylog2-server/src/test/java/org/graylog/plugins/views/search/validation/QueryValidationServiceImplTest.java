package org.graylog.plugins.views.search.validation;

import edu.emory.mathcs.backport.java.util.Collections;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.QueryStringDecorators;
import org.graylog.plugins.views.search.validation.subvalidators.LuceneQuerySubValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class QueryValidationServiceImplTest {

    private QueryValidationServiceImpl toTest;
    private LuceneQuerySubValidator luceneQuerySubValidator;
    private QueryStringDecorators queryStringDecoratorsSubValidator;
    private ValidationRequest validationRequest;

    @BeforeEach
    public void setUp() {
        luceneQuerySubValidator = mock(LuceneQuerySubValidator.class);
        queryStringDecoratorsSubValidator = mock(QueryStringDecorators.class);
        validationRequest = mock(ValidationRequest.class);
        when(validationRequest.query()).thenReturn(ElasticsearchQueryString.of("nevermind"));

        toTest = new QueryValidationServiceImpl(luceneQuerySubValidator, queryStringDecoratorsSubValidator);
    }

    @Test
    public void returnsOkResponseOnEmptyQuery() {
        when(validationRequest.query()).thenReturn(ElasticsearchQueryString.of(""));
        final ValidationResponse validationResponse = toTest.validate(validationRequest);
        assertEquals(ValidationStatus.OK, validationResponse.status());
    }

    @Test
    public void returnsQueryDecoratorsValidationResponseIfItIsNotOk() {
        final ValidationResponse queryStringDecoratorsValidationResponse = ValidationResponse.create(ValidationStatus.ERROR,
                Collections.singletonList(ValidationMessage.builder(ValidationType.MISSING_LICENSE)
                        .errorMessage("It is sooo wrong!").build()));
        when(queryStringDecoratorsSubValidator.validate(validationRequest)).thenReturn(queryStringDecoratorsValidationResponse);
        final ValidationResponse validationResponse = toTest.validate(validationRequest);
        assertEquals(queryStringDecoratorsValidationResponse, validationResponse);
        verifyNoInteractions(luceneQuerySubValidator);
    }

    @Test
    public void returnsLuceneValidationResponseIfQueryDecoratorsValidationIsOk() {
        final ValidationResponse queryStringDecoratorsValidationResponse = ValidationResponse.create(ValidationStatus.OK, Collections.emptyList());
        final ValidationResponse luceneValidationResponse = ValidationResponse.create(ValidationStatus.ERROR,
                Collections.singletonList(ValidationMessage.builder(ValidationType.QUERY_PARSING_ERROR)
                        .errorMessage("It is sooo wrong!").build()));
        when(queryStringDecoratorsSubValidator.validate(validationRequest)).thenReturn(queryStringDecoratorsValidationResponse);
        when(luceneQuerySubValidator.validate(validationRequest)).thenReturn(luceneValidationResponse);
        final ValidationResponse validationResponse = toTest.validate(validationRequest);
        assertEquals(luceneValidationResponse, validationResponse);
    }
}
