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
package org.graylog2.inputs.syslog.tcp;

import com.google.common.base.Charsets;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.CodecEmbedderException;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class SyslogOctetCountFrameDecoderTest {
    private DecoderEmbedder<ChannelBuffer> embedder;

    @BeforeMethod
    public void setUp() throws Exception {
        embedder = new DecoderEmbedder<ChannelBuffer>(new SyslogOctetCountFrameDecoder());
    }

    @Test
    public void testDecode() throws Exception {
        final ChannelBuffer buf1 = ChannelBuffers.copiedBuffer("123 <45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n", Charsets.UTF_8);
        final ChannelBuffer buf2 = ChannelBuffers.copiedBuffer("186 <45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"2\"] Syslog connection established; fd='9', server='AF_INET(172.17.42.1:6666)', local='AF_INET(0.0.0.0:0)'\n", Charsets.UTF_8);
        final ChannelBuffer buf3 = ChannelBuffers.wrappedBuffer(buf1, buf2, buf1);

        assertTrue(embedder.offer(buf1));
        assertTrue(embedder.offer(buf2));
        assertTrue(embedder.offer(buf3));

        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n");
        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"2\"] Syslog connection established; fd='9', server='AF_INET(172.17.42.1:6666)', local='AF_INET(0.0.0.0:0)'\n");

        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n");
        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"2\"] Syslog connection established; fd='9', server='AF_INET(172.17.42.1:6666)', local='AF_INET(0.0.0.0:0)'\n");
        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n");

        assertNull(embedder.poll());
    }

    @Test
    public void testIncompleteFrameLengthValue() throws Exception {
        final ChannelBuffer buf1 = ChannelBuffers.copiedBuffer("12", Charsets.UTF_8);
        final ChannelBuffer buf2 = ChannelBuffers.copiedBuffer("3 <45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n", Charsets.UTF_8);

        assertFalse(embedder.offer(buf1));
        assertNull(embedder.poll());

        assertTrue(embedder.offer(buf2));
        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n");
    }

    @Test
    public void testIncompleteFrames() throws Exception {
        final ChannelBuffer buf1 = ChannelBuffers.copiedBuffer("123 <45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - ", Charsets.UTF_8);
        final ChannelBuffer buf2 = ChannelBuffers.copiedBuffer("[meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n", Charsets.UTF_8);

        assertFalse(embedder.offer(buf1));
        assertNull(embedder.poll());

        assertTrue(embedder.offer(buf2));
        assertEquals(embedder.poll().toString(Charsets.UTF_8), "<45>1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - [meta sequenceId=\"1\"] syslog-ng starting up; version='3.5.3'\n");
    }

    @Test(expectedExceptions = CodecEmbedderException.class)
    public void testBrokenFrames() throws Exception {
        final ChannelBuffer buf1 = ChannelBuffers.copiedBuffer("1 2014-10-21T10:21:09+00:00 c4dc57ba1ebb syslog-ng 7120 - ", Charsets.UTF_8);

        embedder.offer(buf1);
    }
}