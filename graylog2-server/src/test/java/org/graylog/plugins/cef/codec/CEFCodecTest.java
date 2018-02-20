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
package org.graylog.plugins.cef.codec;

import org.graylog.plugins.cef.parser.MappedMessage;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CEFCodecTest {
    private CEFCodec codec;

    @Before
    public void setUp() {
        codec = new CEFCodec(Configuration.EMPTY_CONFIGURATION);
    }

    @Test
    public void buildMessageSummary() throws Exception {
        final com.github.jcustenborder.cef.Message cefMessage = mock(com.github.jcustenborder.cef.Message.class);
        when(cefMessage.deviceProduct()).thenReturn("product");
        when(cefMessage.deviceEventClassId()).thenReturn("event-class-id");
        when(cefMessage.name()).thenReturn("name");
        when(cefMessage.severity()).thenReturn("High");
        assertEquals("product: [event-class-id, High] name", codec.buildMessageSummary(cefMessage));
    }

    @Test
    public void decideSourceWithoutDeviceAddressReturnsRawMessageRemoteAddress() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.emptyMap());

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("128.66.23.42", 12345));

        // The hostname is unresolved, so we have to add the leading slash. Oh, Java...
        assertEquals("/128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithoutDeviceAddressReturnsCEFHostname() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.host()).thenReturn("128.66.23.42");
        when(cefMessage.mappedExtensions()).thenReturn(Collections.emptyMap());

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));

        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithFullDeviceAddressReturnsExtensionValue() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.singletonMap("deviceAddress", "128.66.23.42"));

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));

        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithShortDeviceAddressReturnsExtensionValue() throws Exception {
        final MappedMessage cefMessage = mock(MappedMessage.class);
        when(cefMessage.mappedExtensions()).thenReturn(Collections.singletonMap("dvc", "128.66.23.42"));

        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));

        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void getAggregator() throws Exception {
        assertNull(codec.getAggregator());
    }
}