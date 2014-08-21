/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.outputs;

import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertNotNull;

@Test
public class GelfOutputTest {
    public void testWrite() throws Exception {
        final GelfTransport transport = mock(GelfTransport.class);
        final Message message = mock(Message.class);
        final GelfMessage gelfMessage = new GelfMessage("Test");

        final GelfOutput gelfOutput = Mockito.spy(new GelfOutput());
        doReturn(transport).when(gelfOutput).buildTransport(any(Configuration.class));
        doReturn(gelfMessage).when(gelfOutput).toGELFMessage(message);

        gelfOutput.write(message);

        verify(transport).send(eq(gelfMessage));
    }

    public void testGetRequestedConfiguration() throws Exception {
        final GelfOutput gelfOutput = new GelfOutput();

        final ConfigurationRequest request = gelfOutput.getRequestedConfiguration();

        assertNotNull(request);
        assertNotNull(request.asList());
    }
}
