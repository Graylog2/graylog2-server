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

import org.graylog2.logmessage.LogMessage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class TokenizerFilterTest {

    @Test
    public void testFilter() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("Ohai I am a message k1=v1 k2=v2 Awesome!");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithKVAtBeginning() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("k1=v1 k2=v2 Awesome!");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithKVAtEnd() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("lolwat Awesome! k1=v1");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(1, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
    }

    @Test
    public void testFilterWithStringInBetween() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("foo k2=v2 lolwat Awesome! k1=v1");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

    @Test
    public void testFilterWithKVOnly() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("k1=v1");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(1, msg.getAdditionalData().size());
        assertEquals("v1", msg.getAdditionalData().get("_k1"));
    }

    @Test
    public void testFilterWithInvalidKVPairs() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("Ohai I am a message and this is a URL: index.php?foo=bar&baz=bar");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWithoutKVPairs() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("trolololololol");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWithOneInvalidKVPair() {
        LogMessage msg = new LogMessage();
        msg.setShortMessage("Ohai I am a message and this is a URL: index.php?foo=bar");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(0, msg.getAdditionalData().size());
    }

    @Test
    public void testFilterWillNotOverwriteExistingAdditionalFields() {
        LogMessage msg = new LogMessage();
        msg.addAdditionalData("_k1", "YOU BETTER NOT OVERWRITE");
        msg.setShortMessage("Ohai I am a message k1=v1 k2=v2 Awesome!");
        TokenizerFilter f = new TokenizerFilter();
        f.filter(msg, null);

        assertEquals(2, msg.getAdditionalData().size());
        assertEquals("YOU BETTER NOT OVERWRITE", msg.getAdditionalData().get("_k1"));
        assertEquals("v2", msg.getAdditionalData().get("_k2"));
    }

}