package org.graylog2.plugin.journal;

import com.google.common.base.Charsets;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.system.NodeId;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class RawMessageTest {

    @Test
    public void minimalEncodeDecode() throws IOException {

        final RawMessage rawMessage = new RawMessage("testmessage".getBytes(Charsets.UTF_8));
        final File tempFile = File.createTempFile("node", "test");
        rawMessage.addSourceNode("inputid", new NodeId(tempFile.getAbsolutePath()), true);
        rawMessage.setCodecName("raw");
        rawMessage.setCodecConfig(Configuration.EMPTY_CONFIGURATION);

        final byte[] encoded = rawMessage.encode();
        final RawMessage decodedMsg = RawMessage.decode(ByteBuffer.wrap(encoded), 1);

        assertNotNull(decodedMsg);
        assertEquals(decodedMsg.getPayload(), "testmessage".getBytes(Charsets.UTF_8));
        assertEquals(decodedMsg.getCodecName(), "raw");

    }

}