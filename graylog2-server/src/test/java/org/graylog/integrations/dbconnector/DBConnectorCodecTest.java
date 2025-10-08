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
package org.graylog.integrations.dbconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.NodeId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorCodecTest {
    DBConnectorCodec cut;

    @Mock
    Configuration mockConfiguration;

    ObjectMapper mapper = new ObjectMapper();
    RawMessage rawMessage;
    Message message;

    private final MessageFactory messageFactory = new TestMessageFactory();

    // Set up and tear down
    @Before
    public void setUp() {
        cut = new DBConnectorCodec(mockConfiguration, mapper, messageFactory);
    }

    // Test Cases
    @Test(expected = NullPointerException.class)
    public void decode_throwsException_whenNullMessageProvided() throws MisfireException {
        givenNullRawMessage();
        whenDecodeIsCalled();
    }

    @Test
    public void decode_returnsSampleLog() {
        givenLogs();
        whenDecodeIsCalled();
        thenMessageFieldIs("app", "contenttypes");

    }

    @Test
    public void test_decode_when_OverrideSourceIsNotConfigured() {
        givenGoodConfiguration();
        givenLogs();
        whenDecodeIsCalled();
        assertEquals(message.getSource(), "localhost/test_db/test_table");
    }

    @Test
    public void test_decode_when_OverrideSourceIsConfigured() {
        givenGoodConfiguration();
        givenOverrideSource();
        givenLogs();
        whenDecodeIsCalled();
        assertEquals(message.getSource(), "testSource");
    }


    // GIVENs
    private void givenNullRawMessage() {
        rawMessage = null;
    }

    private void givenLogs() {
        rawMessage = new RawMessage("{\"_id\": {\"$oid\": \"6061d63407838e1803685a09\"}, \"id\": 1, \"app\": \"contenttypes\", \"name\": \"0001_initial\", \"applied\": {\"$date\": 1617024564527}}".getBytes(StandardCharsets.UTF_8));
        NodeId nodeId = mock(NodeId.class);
        given(nodeId.getNodeId()).willReturn("test-node-id");
        rawMessage.addSourceNode("InputId", nodeId);

    }

    private void givenGoodConfiguration() {
        given(mockConfiguration.getString(DBConnectorInput.CK_HOSTNAME)).willReturn("localhost");
        given(mockConfiguration.getString(DBConnectorInput.CK_DATABASE_NAME)).willReturn("test_db");
        given(mockConfiguration.getString(DBConnectorInput.CK_TABLE_NAME)).willReturn("test_table");
        given(mockConfiguration.getString(DBConnectorInput.CK_MONGO_COLLECTION_NAME)).willReturn("test_collection");
    }

    private void givenOverrideSource() {
        given(mockConfiguration.getString(DBConnectorInput.CK_OVERRIDE_SOURCE)).willReturn("testSource");
    }

    // WHENs
    private void whenDecodeIsCalled() {
        message = cut.decodeSafe(rawMessage).get();
    }

    // THENs
    private void thenMessageFieldIs(String field, String value) {
        assertThat(message.getFieldAs(String.class, field), is(value));
    }

}
