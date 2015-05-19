/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.outputs;

import com.google.common.collect.ImmutableMap;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GelfOutputTest {
    @Test
    public void testWrite() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final Message message = mock(Message.class);
        final GelfMessage gelfMessage = new GelfMessage("Test");
        final Configuration configuration = new Configuration(ImmutableMap.<String, Object>of(
                "hostname", "localhost",
                "protocol", "tcp",
                "port", 12201));

        final GelfOutput gelfOutput = Mockito.spy(new GelfOutput(configuration, transport));
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
        final Configuration configuration = new Configuration(ImmutableMap.<String, Object>of(
                "hostname", "localhost",
                "protocol", "tcp",
                "port", 12201));
        final GelfOutput gelfOutput = new GelfOutput(configuration, transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals(gelfMessage.getTimestamp(), now.getMillis() / 1000.0d, 0.0d);
    }

    @Test
    public void testToGELFMessageFullMessage() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final Configuration configuration = new Configuration(ImmutableMap.<String, Object>of(
                "hostname", "localhost",
                "protocol", "tcp",
                "port", 12201));
        final GelfOutput gelfOutput = new GelfOutput(configuration, transport);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Message message = new Message("Test", "Source", now);
        message.addField(Message.FIELD_FULL_MESSAGE, "Full Message");

        final GelfMessage gelfMessage = gelfOutput.toGELFMessage(message);

        assertEquals("Full Message", gelfMessage.getFullMessage());
    }
}
