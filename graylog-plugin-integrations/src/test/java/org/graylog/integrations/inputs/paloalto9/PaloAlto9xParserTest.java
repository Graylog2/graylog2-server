package org.graylog.integrations.inputs.paloalto9;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageType;
import org.graylog.integrations.inputs.paloalto.PaloAltoTypeParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PaloAlto9xParserTest {
    private static final ImmutableList<String> TEST_FIELD_LIST = ImmutableList.of("Foo", "Bar", "Baz");
    private static final ImmutableMap<String,Object> TEST_FIELD_MAP = ImmutableMap.of("field_one", "value_one",
            "field_two", "value_two",
            "field_three", Long.valueOf(3L));

    // Code Under Test
    PaloAlto9xParser cut;

    // Mock Objects
    @Mock PaloAltoTypeParser mockConfigParser;
    @Mock PaloAltoTypeParser mockCorrelationParser;
    @Mock PaloAltoTypeParser mockGlobalProtectParser;
    @Mock PaloAltoTypeParser mockHipParser;
    @Mock PaloAltoTypeParser mockSystemParser;
    @Mock PaloAltoTypeParser mockThreatParser;
    @Mock PaloAltoTypeParser mockTrafficParser;

    // Test Objects
    PaloAltoMessageType inputMessageType;
    List<String> inputFields;
    ImmutableMap<String, Object> outputFields;

    // Set Up
    @Before
    public void setUp() {
        cut = new PaloAlto9xParser(mockConfigParser,
                mockCorrelationParser,
                mockGlobalProtectParser,
                mockHipParser,
                mockSystemParser,
                mockThreatParser,
                mockTrafficParser);
    }

    // Test Cases
    @Test
    public void parseFields_returnExpectedFieldMap_whenConfigMessageType() {
        givenInputFieldType(PaloAltoMessageType.CONFIG);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenConfigParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_whenCorrelationMessageType() {
        givenInputFieldType(PaloAltoMessageType.CORRELATION);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenCorrelationParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_whenGlobalProtectMessageType() {
        givenInputFieldType(PaloAltoMessageType.GLOBAL_PROTECT);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenGlobalProtectParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_whenHipMessageType() {
        givenInputFieldType(PaloAltoMessageType.HIP);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenHipParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_whenSystemMessageType() {
        givenInputFieldType(PaloAltoMessageType.SYSTEM);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenSystemParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_whenThreatMessageType() {
        givenInputFieldType(PaloAltoMessageType.THREAT);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenThreatParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_whenTrafficMessageType() {
        givenInputFieldType(PaloAltoMessageType.TRAFFIC);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenTrafficParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnsEmptyMap_whenNoParserFound() {
        givenInputFieldType(null);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenNoParserUsed();
        thenEmptyMapReturned();
    }

    // GIVENs
    private void givenInputFieldType(PaloAltoMessageType type) {
        inputMessageType = type;
    }

    private void givenGoodInputFields() {
        inputFields = TEST_FIELD_LIST;
    }

    private void givenGoodParsers() {
        given(mockConfigParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockCorrelationParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockGlobalProtectParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockHipParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockSystemParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockThreatParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockTrafficParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
    }

    // WHENs
    private void whenParseFieldsIsCalled() {
        outputFields = cut.parseFields(inputMessageType, inputFields);
    }

    // THENs
    private void thenConfigParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockConfigParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenCorrelationParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockCorrelationParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenGlobalProtectParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockGlobalProtectParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenHipParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockHipParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenSystemParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockSystemParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenThreatParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockThreatParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenTrafficParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockTrafficParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
    }

    private void thenNoParserUsed() {
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenExpectedOutputIsReturned() {
        assertThat(outputFields, notNullValue());
        assertThat(outputFields, is(TEST_FIELD_MAP));
    }

    private void thenEmptyMapReturned() {
        assertThat(outputFields, notNullValue());
        assertThat(outputFields.size(), is(0));
    }
}
