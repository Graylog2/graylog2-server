/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.journal;

import com.google.common.base.Charsets;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.system.NodeId;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

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
        final RawMessage decodedMsg = RawMessage.decode(encoded, 1);

        assertNotNull(decodedMsg);
        assertEquals(decodedMsg.getPayload(), "testmessage".getBytes(Charsets.UTF_8));
        assertEquals(decodedMsg.getCodecName(), "raw");

    }

}