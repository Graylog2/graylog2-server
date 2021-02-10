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
    @Mock PaloAltoTypeParser mockGlobalProtectPre913Parser;
    @Mock PaloAltoTypeParser mockGlobalProtect913Parser;
    @Mock PaloAltoTypeParser mockHipParser;
    @Mock PaloAltoTypeParser mockSystemParser;
    @Mock PaloAltoTypeParser mockThreatParser;
    @Mock PaloAltoTypeParser mockTrafficParser;
    @Mock PaloAltoTypeParser mockUserIdParser;

    // Test Objects
    PaloAltoMessageType inputMessageType;
    List<String> inputFields;
    ImmutableMap<String, Object> outputFields;

    // Set Up
    @Before
    public void setUp() {
        cut = new PaloAlto9xParser(mockConfigParser,
                mockCorrelationParser,
                mockGlobalProtectPre913Parser,
                mockGlobalProtect913Parser,
                mockHipParser,
                mockSystemParser,
                mockThreatParser,
                mockTrafficParser,
                mockUserIdParser);
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
    public void parseFields_returnExpectedFieldMap_whenPre913GlobalProtectMessageType() {
        givenInputFieldType(PaloAltoMessageType.GLOBAL_PROTECT_PRE_9_1_3);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenPre913GlobalProtectParserIsUsed();
        thenExpectedOutputIsReturned();
    }

    @Test
    public void parseFields_returnExpectedFieldMap_when913GlobalProtectMessageType() {
        givenInputFieldType(PaloAltoMessageType.GLOBAL_PROTECT_9_1_3);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        then913GlobalProtectParserIsUsed();
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
    public void parseFields_returnExpectedFieldMap_whenUserIdMessageType() {
        givenInputFieldType(PaloAltoMessageType.USERID);
        givenGoodInputFields();
        givenGoodParsers();

        whenParseFieldsIsCalled();

        thenUserIdParserIsUsed();
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
        given(mockGlobalProtectPre913Parser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockGlobalProtect913Parser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockHipParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockSystemParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockThreatParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockTrafficParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
        given(mockUserIdParser.parseFields(anyList())).willReturn(TEST_FIELD_MAP);
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
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenCorrelationParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockCorrelationParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenPre913GlobalProtectParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockGlobalProtectPre913Parser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void then913GlobalProtectParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockGlobalProtect913Parser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenHipParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockHipParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenSystemParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockSystemParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenThreatParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockThreatParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockTrafficParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenTrafficParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockTrafficParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockUserIdParser);
    }

    private void thenUserIdParserIsUsed() {
        ArgumentCaptor<List<String>> fieldsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockUserIdParser, times(1)).parseFields(fieldsCaptor.capture());
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
        verifyNoMoreInteractions(mockHipParser);
        verifyNoMoreInteractions(mockSystemParser);
        verifyNoMoreInteractions(mockThreatParser);
        verifyNoMoreInteractions(mockTrafficParser);
    }

    private void thenNoParserUsed() {
        verifyNoMoreInteractions(mockConfigParser);
        verifyNoMoreInteractions(mockCorrelationParser);
        verifyNoMoreInteractions(mockGlobalProtectPre913Parser);
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
