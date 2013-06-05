/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.streams.matchers;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MatcherBaseTest {

    @Test
    public void testGetInt() throws Exception {
        MatcherBase mb  = new MatcherBase();

        assertEquals(null, mb.getInt(null));

        assertEquals((Integer) 0, mb.getInt(0));
        assertEquals((Integer) 1, mb.getInt(1));
        assertEquals((Integer) 9001, mb.getInt(9001));

        assertEquals((Integer) 1253453, mb.getInt((long) 1253453));
        assertEquals(null, mb.getInt((double) 5));
        assertEquals(null, mb.getInt(18.2));

        assertEquals((Integer) 88, mb.getInt("88"));
        assertEquals(null, mb.getInt("lol NOT A NUMBER"));

        assertEquals(null, mb.getInt(new HashMap<String, String>()));
    }

}
