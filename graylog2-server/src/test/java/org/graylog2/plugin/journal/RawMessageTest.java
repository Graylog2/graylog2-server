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
package org.graylog2.plugin.journal;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.system.NodeId;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RawMessageTest {
    @Test
    public void minimalEncodeDecode() throws IOException {
        final RawMessage rawMessage = new RawMessage("testmessage".getBytes(StandardCharsets.UTF_8));
        final File tempFile = File.createTempFile("node", "test");
        rawMessage.addSourceNode("inputid", new NodeId(tempFile.getAbsolutePath()));
        rawMessage.setCodecName("raw");
        rawMessage.setCodecConfig(Configuration.EMPTY_CONFIGURATION);

        final byte[] encoded = rawMessage.encode();
        final RawMessage decodedMsg = RawMessage.decode(encoded, 1);

        assertNotNull(decodedMsg);
        assertArrayEquals("testmessage".getBytes(StandardCharsets.UTF_8), decodedMsg.getPayload());
        assertEquals("raw", decodedMsg.getCodecName());
    }
}
