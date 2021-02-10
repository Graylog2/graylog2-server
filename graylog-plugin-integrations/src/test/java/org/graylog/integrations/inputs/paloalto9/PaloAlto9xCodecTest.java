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
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageBase;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageType;
import org.graylog.integrations.inputs.paloalto.PaloAltoParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PaloAlto9xCodecTest {
    private static final String TEST_SOURCE = "Test Source";
    private static final DateTime TEST_DATE_TIME = DateTime.now();
    private static final String TEST_RAW_MESSAGE = "Foo,Bar,Baz,This,That,GLOBALPROTECT";
    private static final ImmutableList<String> TEST_FIELD_LIST = ImmutableList.of("Foo", "Bar", "Baz", "Three", "Four", "GLOBALPROTECT");
    private static final ImmutableMap<String,Object> TEST_FIELD_MAP = ImmutableMap.of("field_one", "value_one",
            "field_two", "value_two",
            "field_three", Long.valueOf(3L));

    // Code Under Test
    @InjectMocks PaloAlto9xCodec cut;

    // Mock Objects
    @Mock Configuration mockConfig;
    @Mock PaloAltoParser mockRawParser;
    @Mock PaloAlto9xParser mockPaloParser;

    // Test Objects
    RawMessage in;
    Message out;

    // Test Cases
    @Test
    public void decode_runsSuccessfully_whenGoodConfigInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("CONFIG");
        givenStoreFullMessage(false);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.CONFIG);
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodCorrelationInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("CORRELATION");
        givenStoreFullMessage(true);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.CORRELATION);
        thenOutputMessageContainsExpectedFields(true);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodGlobalProtect90Input() {
        givenGoodInputRawMessage();
        givenPaloMessageType("GLOBALPROTECT 9.0");
        givenStoreFullMessage(false);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.GLOBAL_PROTECT_PRE_9_1_3);
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodGlobalProtect913Input() {
        givenGoodInputRawMessage();
        givenPaloMessageType("GLOBALPROTECT");
        givenStoreFullMessage(false);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.GLOBAL_PROTECT_9_1_3);
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodHipMatchInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("HIP-MATCH");
        givenStoreFullMessage(true);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.HIP);
        thenOutputMessageContainsExpectedFields(true);
    }

    @Test
    public void decode_runsSuccessfully_whenNonStandardHipMatchInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("HIPMATCH");
        givenStoreFullMessage(false);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.HIP);
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodSystemInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("SYSTEM");
        givenStoreFullMessage(true);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.SYSTEM);
        thenOutputMessageContainsExpectedFields(true);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodThreatInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("THREAT");
        givenStoreFullMessage(false);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.THREAT);
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodTrafficInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("TRAFFIC");
        givenStoreFullMessage(true);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.TRAFFIC);
        thenOutputMessageContainsExpectedFields(true);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodUserIdInput() {
        givenGoodInputRawMessage();
        givenPaloMessageType("USERID");
        givenStoreFullMessage(false);
        givenGoodFieldProducer();

        whenDecodeIsCalled();

        thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType.USERID);
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_returnsNull_whenRawPaloParseFails() {
        givenGoodInputRawMessage();
        givenRawParserReturnsNull();

        whenDecodeIsCalled();

        thenOutputMessageIsNull();
    }

    // GIVENs
    private void givenGoodInputRawMessage() {
        in = new RawMessage(TEST_RAW_MESSAGE.getBytes());
    }

    private void givenRawParserReturnsNull() {
        given(mockRawParser.parse(anyString())).willReturn(null);
    }

    private void givenPaloMessageType(String panType) {
        PaloAltoMessageBase foo = PaloAltoMessageBase.create(TEST_SOURCE, TEST_DATE_TIME, TEST_RAW_MESSAGE, panType,
                TEST_FIELD_LIST);
        given(mockRawParser.parse(anyString())).willReturn(foo);
    }

    private void givenStoreFullMessage(boolean storeFullMessage) {
        given(mockConfig.getBoolean(PaloAlto9xCodec.CK_STORE_FULL_MESSAGE)).willReturn(storeFullMessage);
    }

    private void givenGoodFieldProducer() {
        given(mockPaloParser.parseFields(any(PaloAltoMessageType.class), anyList())).willReturn(TEST_FIELD_MAP);
    }

    // WHENs
    private void whenDecodeIsCalled() {
        out = cut.decode(in);
    }

    // THENs
    private void thenPaloParserCalledWithPaloMessageType(PaloAltoMessageType type) {
        ArgumentCaptor<PaloAltoMessageType> typeCaptor = ArgumentCaptor.forClass(PaloAltoMessageType.class);
        ArgumentCaptor<ImmutableList<String>> fieldsCaptor = ArgumentCaptor.forClass(ImmutableList.class);

        verify(mockPaloParser, times(1)).parseFields(typeCaptor.capture(), fieldsCaptor.capture());

        assertThat(typeCaptor.getValue(), is(type));
        assertThat(fieldsCaptor.getValue(), is(TEST_FIELD_LIST));
    }

    private void thenOutputMessageContainsExpectedFields(boolean shouldContainFullMessage) {
        assertThat(out, notNullValue());

        assertThat(out.getField("field_one"), is("value_one"));
        assertThat(out.getField("field_two"), is("value_two"));
        assertThat(out.getField("field_three"), is(Long.valueOf(3L)));

        if (shouldContainFullMessage) {
            assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(TEST_RAW_MESSAGE));
        } else {
            assertThat(out.getField(Message.FIELD_FULL_MESSAGE), nullValue());
        }
    }

    private void thenOutputMessageIsNull() {
        assertThat(out, nullValue());
    }
}
