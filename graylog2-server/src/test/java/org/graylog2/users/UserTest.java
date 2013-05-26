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
package org.graylog2.users;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class UserTest {

    @Test
    public void testSaltPass() throws Exception {
        String salt = "uQTMv2b5JIaoi042pYUHl7lgqwUMLO61Vo5hq2fudATkymPlHC7OOFKSp266hpBo";
        assertEquals("foobar" + salt , User.saltPass("foobar", salt));
    }

    @Test(expected=RuntimeException.class)
    public void testSaltPassRefusesNullPass() throws Exception {
        User.saltPass(null, "foo");
    }

    @Test(expected=RuntimeException.class)
    public void testSaltPassRefusesEmptyPass() throws Exception {
        User.saltPass("", "foo");
    }

    @Test(expected=RuntimeException.class)
    public void testSaltPassRefusesNullSalt() throws Exception {
        User.saltPass("foo", null);
    }

    @Test(expected=RuntimeException.class)
    public void testSaltPassRefusesEmptySalt() throws Exception {
        User.saltPass("foo", null);
    }

}
