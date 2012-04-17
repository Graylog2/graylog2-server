/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.inputs.gelf;

import org.graylog2.Tools;
import org.graylog2.GraylogServerStub;
import org.graylog2.TestHelper;
import org.graylog2.logmessage.LogMessage;
import org.junit.Test;
import static org.junit.Assert.*;

public class GELFProcessorTest {

    public final static double usedTimestamp = Tools.getUTCTimestampWithMilliseconds();
    public final static String GELF_JSON_COMPLETE = "{\"short_message\":\"foo\",\"full_message\":\"foo\nzomg\",\"facility\":"
            + "\"test\",\"level\":3,\"line\":23,\"file\":\"lol.js\",\"host\":\"bar\",\"timestamp\": " + usedTimestamp + ",\"_lol_utf8\":\"ü\",\"_foo\":\"bar\"}";

    public final static String GELF_JSON_INCOMPLETE = "{\"short_message\":\"foo\",\"host\":\"bar\"}";

    public final static String GELF_JSON_INCOMPLETE_WITH_ID = "{\"short_message\":\"foo\",\"host\":\"bar\",\"_id\":\":7\",\"_something\":\"foo\"}";

    public final static String GELF_JSON_INCOMPLETE_WITH_NON_STANDARD_FIELD = "{\"short_message\":\"foo\",\"host\":\"bar\",\"lol_not_allowed\":\":7\",\"_something\":\"foo\"}";

    @Test
    public void testMessageReceived() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_COMPLETE)));
        processor.messageReceived(new GELFMessage(TestHelper.gzipCompress(GELF_JSON_COMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;
        
        assertEquals(2, serverStub.callsToProcessBufferInserter);
        assertEquals("foo", lm.getShortMessage());
        assertEquals("foo\nzomg", lm.getFullMessage());
        assertEquals("test", lm.getFacility());
        assertEquals(3, lm.getLevel());
        assertEquals("bar", lm.getHost());
        assertEquals("lol.js", lm.getFile());
        assertEquals(23, lm.getLine());
        assertEquals(usedTimestamp, lm.getCreatedAt(), 1e-8);
        assertEquals("ü", lm.getAdditionalData().get("_lol_utf8"));
        assertEquals("bar", lm.getAdditionalData().get("_foo"));
        assertEquals(2, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedSetsCreatedAtToNowIfNotSet() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals(Tools.getUTCTimestampWithMilliseconds(), lm.getCreatedAt(), 2);
    }

    @Test
    public void testMessageReceivedSetsLevelToDefaultIfNotSet() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals(LogMessage.STANDARD_LEVEL, lm.getLevel());
    }

    @Test
    public void testMessageReceivedSetsFacilityToDefaultIfNotSet() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertEquals(LogMessage.STANDARD_FACILITY, lm.getFacility());
    }

    @Test
    public void testMessageReceivedSkipsSettingIDField() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE_WITH_ID)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertNull(lm.getAdditionalData().get("_id"));
        assertEquals("foo", lm.getAdditionalData().get("_something"));
        assertEquals(1, lm.getAdditionalData().size());
    }

    @Test
    public void testMessageReceivedSkipsNonStandardFields() throws Exception {
        GraylogServerStub serverStub = new GraylogServerStub();
        GELFProcessor processor = new GELFProcessor(serverStub);

        processor.messageReceived(new GELFMessage(TestHelper.zlibCompress(GELF_JSON_INCOMPLETE_WITH_NON_STANDARD_FIELD)));
        // All GELF types are tested in GELFMessageTest.

        LogMessage lm = serverStub.lastInsertedToProcessBuffer;

        assertEquals(1, serverStub.callsToProcessBufferInserter);
        assertNull(lm.getAdditionalData().get("lol_not_allowed"));
        assertEquals("foo", lm.getAdditionalData().get("_something"));
        assertEquals(1, lm.getAdditionalData().size());
    }
}