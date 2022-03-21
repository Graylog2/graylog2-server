package org.graylog.plugins.views.search.validation.subvalidators;

import com.google.common.collect.ImmutableList;
import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationResponse;
import org.graylog.plugins.views.search.validation.ValidationStatus;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LuceneQuerySubValidatorTest {

    private LuceneQueryParser luceneQueryParser;
    private UnknownFieldsIdentifier unknownFieldsIdentifier;
    private ValidationExplanationCreator validationExplanationCreator;
    private ValidationRequest validationRequest;
    private ParsedQuery parsedQuery;

    private LuceneQuerySubValidator toTest;

    private final String queryString = "search";

    @BeforeEach
    public void setUp() throws Exception {
        luceneQueryParser = mock(LuceneQueryParser.class);
        unknownFieldsIdentifier = mock(UnknownFieldsIdentifier.class);
        validationExplanationCreator = mock(ValidationExplanationCreator.class);
        validationRequest = mock(ValidationRequest.class);
        parsedQuery = mock(ParsedQuery.class);

        when(validationRequest.query()).thenReturn(ElasticsearchQueryString.of(queryString));
        when(luceneQueryParser.parse(queryString)).thenReturn(parsedQuery);
        when(luceneQueryParser.parse(queryString)).thenReturn(parsedQuery);

        toTest = new LuceneQuerySubValidator(luceneQueryParser, unknownFieldsIdentifier, validationExplanationCreator);
    }

    @Test
    public void returnsErrorOnParsingException() throws Exception {
        when(luceneQueryParser.parse(queryString)).thenThrow(new ParseException("Gosh..."));
        when(validationExplanationCreator.getExceptionExplanations(any()))
                .thenReturn(Collections.singletonList(ValidationMessage.builder(ValidationType.QUERY_PARSING_ERROR).errorMessage("Oh noes!").build()));

        final ValidationResponse validationResponse = toTest.validate(validationRequest);
        assertEquals(ValidationStatus.ERROR, validationResponse.status());
        final List<ValidationMessage> explanations = validationResponse.explanations();
        assertEquals(1, explanations.size());
        assertEquals(ValidationType.QUERY_PARSING_ERROR, explanations.get(0).validationType());
        assertEquals("Oh noes!", explanations.get(0).errorMessage());

    }

    @Test
    public void returnsOkOnNoProblems() throws Exception {
        final ImmutableList<ParsedTerm> parsedQueryTerms = ImmutableList.of(ParsedTerm.create("field", "value"));
        when(unknownFieldsIdentifier.identifyUnknownFields(validationRequest, parsedQueryTerms)).thenReturn(Collections.emptyList());
        when(parsedQuery.invalidOperators()).thenReturn(Collections.emptyList());
        when(parsedQuery.terms()).thenReturn(parsedQueryTerms);
        when(validationExplanationCreator.getVerificationExplanations(argThat(List::isEmpty), argThat(List::isEmpty))).thenReturn(Collections.emptyList());

        final ValidationResponse validationResponse = toTest.validate(validationRequest);
        assertEquals(ValidationStatus.OK, validationResponse.status());
    }

    @Test
    public void returnsWarningOnNoProblems() throws Exception {
        final ImmutableList<ParsedTerm> parsedQueryTerms = ImmutableList.of(ParsedTerm.create("field", "value"));
        final List<ParsedTerm> invalidOperators = Collections.singletonList(ParsedTerm.create("invalidOperator", "val"));
        final List<ParsedTerm> unknownFields = Collections.singletonList(ParsedTerm.create("unknownField", "nvmd"));
        when(unknownFieldsIdentifier.identifyUnknownFields(validationRequest, parsedQueryTerms)).thenReturn(unknownFields);
        when(parsedQuery.invalidOperators()).thenReturn(invalidOperators);
        when(parsedQuery.terms()).thenReturn(parsedQueryTerms);
        final ValidationMessage unknownFieldMsg = ValidationMessage.builder(ValidationType.UNKNOWN_FIELD).errorMessage("No!").build();
        final ValidationMessage invalidOperatorMsg = ValidationMessage.builder(ValidationType.INVALID_OPERATOR).errorMessage("No, please stop typing wrong queries!").build();
        when(validationExplanationCreator.getVerificationExplanations(unknownFields, invalidOperators)).thenReturn(
                Arrays.asList(unknownFieldMsg, invalidOperatorMsg));

        final ValidationResponse validationResponse = toTest.validate(validationRequest);
        assertEquals(ValidationStatus.WARNING, validationResponse.status());
        assertTrue(validationResponse.explanations().contains(unknownFieldMsg));
        assertTrue(validationResponse.explanations().contains(invalidOperatorMsg));
    }
}
