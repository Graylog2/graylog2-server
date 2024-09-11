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
package org.graylog.integrations.inputs.paloalto11;

import com.google.common.collect.ImmutableList;
import org.graylog.integrations.inputs.paloalto.PaloAltoMessageBase;
import org.graylog.integrations.inputs.paloalto.PaloAltoParser;
import org.graylog.schema.EventFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class PaloAlto11xCodecTest {
    private static final String TEST_SOURCE = "Test Source";
    private static final DateTime TEST_DATE_TIME = DateTime.now(DateTimeZone.UTC);
    private static final String TEST_RAW_MESSAGE = "Foo,Bar,Baz,This,That,GLOBALPROTECT";
    private static final ImmutableList<String> TEST_FIELD_LIST = ImmutableList.of("Foo", "Bar", "Baz", "Three", "Four", "GLOBALPROTECT");
    private final MessageFactory messageFactory = new TestMessageFactory();

    // Code Under Test
    PaloAlto11xCodec cut;

    // Mock Objects
    @Mock
    Configuration mockConfig;
    @Mock
    PaloAltoParser mockRawParser;

    // Test Objects
    RawMessage in;
    Message out;

    @Before
    public void setUp() throws Exception {
        this.cut = new PaloAlto11xCodec(mockConfig, mockRawParser, messageFactory);
    }

    @Test
    public void decode_runsSuccessfully_whenGoodInput() {
        givenGoodInputRawMessage();
        givenRawParserReturnsValidMessage();
        givenStoreFullMessage(false);
        whenDecodeIsCalled();
        thenOutputMessageContainsExpectedFields(false);
    }

    @Test
    public void decode_returnsDefaults_whenRawPaloParseFails() {
        givenGoodInputRawMessage();
        givenRawParserReturnsNull();
        whenDecodeIsCalled();
        thenOutputMessageIsDefaults();
    }

    // GIVENs
    private void givenGoodInputRawMessage() {
        in = new RawMessage(TEST_RAW_MESSAGE.getBytes(StandardCharsets.UTF_8));
    }

    private void givenRawParserReturnsValidMessage() {
        PaloAltoMessageBase foo = PaloAltoMessageBase.create(TEST_SOURCE, TEST_DATE_TIME, TEST_RAW_MESSAGE, "SYSTEM",
                TEST_FIELD_LIST);
        given(mockRawParser.parse(anyString(), any(DateTimeZone.class))).willReturn(foo);
    }

    private void givenRawParserReturnsNull() {
        given(mockRawParser.parse(anyString(), any(DateTimeZone.class))).willReturn(null);
    }

    private void givenStoreFullMessage(boolean storeFullMessage) {
        given(mockConfig.getBoolean(PaloAlto11xCodec.CK_STORE_FULL_MESSAGE)).willReturn(storeFullMessage);
    }

    // WHENs
    private void whenDecodeIsCalled() {
        out = cut.decode(in);
    }

    private void thenOutputMessageContainsExpectedFields(boolean shouldContainFullMessage) {
        assertNotNull(out);
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is(PaloAlto11xCodec.EVENT_SOURCE_PRODUCT_NAME));
        assertThat(out.getField(VendorFields.VENDOR_SUBTYPE), is("SYSTEM"));

        if (shouldContainFullMessage) {
            assertThat(out.getField(Message.FIELD_FULL_MESSAGE), is(TEST_RAW_MESSAGE));
        } else {
            assertThat(out.getField(Message.FIELD_FULL_MESSAGE), nullValue());
        }
    }

    private void thenOutputMessageIsDefaults() {
        assertNotNull(out);
        assertThat(out.getField(EventFields.EVENT_SOURCE_PRODUCT), is(PaloAlto11xCodec.EVENT_SOURCE_PRODUCT_NAME));
        assertThat(out.getField(VendorFields.VENDOR_SUBTYPE), is(PaloAlto11xCodec.UNKNOWN));
        assertThat(out.getField(Message.FIELD_MESSAGE), is(TEST_RAW_MESSAGE));
        assertThat(out.getField(Message.FIELD_TIMESTAMP), notNullValue());
    }
}
