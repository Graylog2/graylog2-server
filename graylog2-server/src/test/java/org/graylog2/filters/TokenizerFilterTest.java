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
import org.graylog2.plugin.Message;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author lennart.koopmann
 */
public class TokenizerFilterTest {

    @Test
    public void testFilter() {
        Message msg = new Message("Ohai I am a message k1=v1 k2=v2 Awesome!", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(6, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
        assertEquals("v2", msg.getField("k2"));
    }

    @Test
    public void testFilterWithKVAtBeginning() {
        Message msg = new Message("k1=v1 k2=v2 Awesome!", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(6, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
        assertEquals("v2", msg.getField("k2"));
    }

    @Test
    public void testFilterWithKVAtEnd() {
        Message msg = new Message("lolwat Awesome! k1=v1", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(5, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
    }

    @Test
    public void testFilterWithStringInBetween() {
        Message msg = new Message("foo k2=v2 lolwat Awesome! k1=v1", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(6, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
        assertEquals("v2", msg.getField("k2"));
    }

    @Test
    public void testFilterWithKVOnly() {
        Message msg = new Message("k1=v1", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(5, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
    }

    @Test
    public void testFilterWithInvalidKVPairs() {
        Message msg = new Message("Ohai I am a message and this is a URL: index.php?foo=bar&baz=bar", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(4, msg.getFields().size());
    }

    @Test
    public void testFilterWithoutKVPairs() {
        Message msg = new Message("trolololololol", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(4, msg.getFields().size());
    }

    @Test
    public void testFilterWithOneInvalidKVPair() {
        Message msg = new Message("Ohai I am a message and this is a URL: index.php?foo=bar", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(4, msg.getFields().size());
    }

    @Test
    public void testFilterWillNotOverwriteExistingAdditionalFields() {
        Message msg = new Message("Ohai I am a message k1=v1 k2=v2 Awesome!", "foo", 0);
        msg.addField("k1", "YOU BETTER NOT OVERWRITE");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(6, msg.getFields().size());
        assertEquals("YOU BETTER NOT OVERWRITE", msg.getField("k1"));
        assertEquals("v2", msg.getField("k2"));
    }

    @Test
    public void testFilterWithWhitespaceAroundKVNoException() {
        Message msg = new Message("k1 = ", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();

        Logger logger = Logger.getLogger(TokenizerFilter.class);
        TestAppender testAppender = new TestAppender();
        logger.addAppender(testAppender);

        f.filter(msg, new GraylogServerStub());

        assertEquals("Should not log anything", 0, testAppender.getEvents().size());
        assertEquals(4, msg.getFields().size());
    }

    @Test
    public void testFilterWithWhitespaceAroundKV() {
        Message msg = new Message("otters in k1 = v1 k2= v2 k3 =v3 k4=v4 more otters", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(8, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
        assertEquals("v2", msg.getField("k2"));
        assertEquals("v3", msg.getField("k3"));
        assertEquals("v4", msg.getField("k4"));
    }

    @Test
    public void testFilterWithQuotedValue() {
        Message msg = new Message("otters in k1=\"v1\" more otters", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertEquals(5, msg.getFields().size());
        assertEquals("v1", msg.getField("k1"));
    }

   @Test
    public void testFilterWithIDAdditionalField() {
        Message msg = new Message("otters _id=123 more otters", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        assertTrue(msg.getField("_id") != "123");
    }
   
   @Test
   public void testFilterWithConflictingAdditionalFields() {
        Message msg = new Message("otters id=lolwut _id=123 _ttl=123 more otters", "foo", 0);
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, new GraylogServerStub());

        // id is fine (_id is not)
        assertEquals("lolwut", msg.getField("id"));
        
        // TTL and ID are protected and should not get accepted.
        assertTrue(msg.getField("_id") != "123");
        assertEquals(false, msg.getFields().containsKey("_ttl"));
   }

}
