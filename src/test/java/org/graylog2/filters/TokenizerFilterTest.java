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

package org.graylog2.filters;

import org.apache.log4j.Logger;
import org.graylog2.GraylogServerStub;
import org.graylog2.TestAppender;
import org.graylog2.logmessage.LogMessageImpl;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author lennart.koopmann
 */
public class TokenizerFilterTest {

    @Test
    public void testFilter() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("Ohai I am a message k1=v1 k2=v2 Awesome!");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithKVAtBeginning() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("k1=v1 k2=v2 Awesome!");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithKVAtEnd() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("lolwat Awesome! k1=v1");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(1, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
    }

    @Test
    public void testFilterWithStringInBetween() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("foo k2=v2 lolwat Awesome! k1=v1");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithKVOnly() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("k1=v1");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(1, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
    }

    @Test
    public void testFilterWithInvalidKVPairs() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("Ohai I am a message and this is a URL: index.php?foo=bar&baz=bar");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWithoutKVPairs() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("trolololololol");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWithOneInvalidKVPair() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("Ohai I am a message and this is a URL: index.php?foo=bar");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWillNotOverwriteExistingAdditionalFields() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.addAdditionalData("_k1", "YOU BETTER NOT OVERWRITE");
        msg.setShortMessage("Ohai I am a message k1=v1 k2=v2 Awesome!");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("YOU BETTER NOT OVERWRITE", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithWhitespaceAroundKVNoException() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("k1 = ");
        TokenizerFilter f = new TokenizerFilter();

        Logger logger = Logger.getLogger(TokenizerFilter.class);
        TestAppender testAppender = new TestAppender();
        logger.addAppender(testAppender);

        f.filter(msg, new GraylogServerStub());

        assertEquals("Should not log anything", 0, testAppender.getEvents().size());
        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWithWhitespaceAroundKV() {
        LogMessageImpl msg = new LogMessageImpl();
        msg.setShortMessage("otters in k1 = v1 k2= v2 k3 =v3 k4=v4 more otters");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals("There should be 4 kv pairs", 4, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
        assertEquals("v3", msg.getAdditionalData().get("_k3"));
        assertEquals("v4", msg.getAdditionalData().get("_k4"));
    }

}