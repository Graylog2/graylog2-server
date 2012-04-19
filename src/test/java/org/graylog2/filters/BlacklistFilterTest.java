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

import java.util.List;
import java.util.ArrayList;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.logmessage.LogMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlacklistFilterTest {

    @Test
    public void testFilterWithOneBlacklist() {
        LogMessage msg = new LogMessage();
        List<Blacklist> stubbedBlacklists = new ArrayList<Blacklist>();

        Blacklist b1 = new StubbedBlacklist(null);

        MessageFilter f = new BlacklistFilterWithStubbedBlacklists(stubbedBlacklists);
        f.filter(msg);

        assertFalse(f.discardMessage());
    }

    @Test
    public void testFilterWithMultipleBlacklist() {
        fail();
        LogMessage msg = new LogMessage();
        List<Blacklist> stubbedBlacklists = new ArrayList<Blacklist>();

        MessageFilter f = new BlacklistFilterWithStubbedBlacklists(stubbedBlacklists);
        f.filter(msg);

        assertFalse(f.discardMessage());
    }

    @Test
    public void testFilterWithEmptyBlacklist() {
        LogMessage msg = new LogMessage();
        List<Blacklist> stubbedBlacklists = new ArrayList<Blacklist>();

        MessageFilter f = new BlacklistFilterWithStubbedBlacklists(stubbedBlacklists);
        f.filter(msg);

        assertFalse(f.discardMessage());
    }

    @Test
    public void testDiscardMessage() {
    }

}