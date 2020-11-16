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
package org.graylog2.outputs;

import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GelfOutputTest {
    @Test
    public void testWrite() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final Message message = mock(Message.class);
        final GelfMessage gelfMessage = new GelfMessage("Test");
        final GelfOutput gelfOutput = Mockito.spy(new GelfOutput(transport));
        doReturn(gelfMessage).when(gelfOutput).toGELFMessage(message);

        gelfOutput.write(message);

        verify(transport).send(eq(gelfMessage));
    }

    @Test
    public void testGetRequestedConfiguration() throws Exception {
        final GelfOutput.Config gelfOutputConfig = new GelfOutput.Config();

        final ConfigurationRequest request = gelfOutputConfig.getRequestedConfiguration();

        assertNotNull(request);
        assertNotNull(request.asList());
    }

    @Test
    public void testToGELFMessageTimestamp() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(gelfMessage.getTimestamp(), now.getMillis() / 1000.0d, 0.0d);
    }

    @Test
    public void testToGELFMessageFullMessage() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField(Message.FIELD_FULL_MESSAGE, "Full Message");

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals("Full Message", gelfMessage.getFullMessage());
    }

    @Test
    public void testToGELFMessageWithValidNumericLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", 6);

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.INFO, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithInvalidNumericLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", -1L);

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.ALERT, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithValidStringLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", "6");

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.INFO, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithInvalidStringLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", "BOOM");

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.ALERT, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithInvalidNumericStringLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", "-1");

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.ALERT, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithInvalidTypeLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", new Object());

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.ALERT, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithNullLevel() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("level", null);

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(GelfMessageLevel.ALERT, gelfMessage.getLevel());
    }

    @Test
    public void testToGELFMessageWithNonStringFacility() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final GelfOutput gelfOutput = new GelfOutput(transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField("facility", 42L);

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(42L, gelfMessage.getAdditionalFields().get("facility"));
    }
}
