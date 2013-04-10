/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.pattern;

import static org.junit.Assert.*;

import org.junit.Test;

public class PatternTest {
    @Test
    public void testPartialRegex() {
        assertTrue(new PartialRegexPattern("foo.bar").matches("asdf foo-bar asdf"));
        assertFalse(new PartialRegexPattern("foo.bar").matches("asdf foobar asdf"));
        assertTrue(new PartialRegexPattern("foo.bar").matches("foo-bar"));
        assertFalse(new PartialRegexPattern("foo.bar").matches("foobar"));
    }
    
    @Test
    public void testPartialString() {
        assertFalse(new PartialStringPattern("foobar").matches("asdf foo-bar asdf"));
        assertTrue(new PartialStringPattern("foobar").matches("asdf foobar asdf"));
        assertFalse(new PartialStringPattern("foobar").matches("foo-bar"));
        assertTrue(new PartialStringPattern("foobar").matches("foobar"));
    }

    @Test
    public void testEntireRegex() {
        assertFalse(new EntireRegexPattern("foo.bar").matches("asdf foo-bar asdf"));
        assertFalse(new EntireRegexPattern("foo.bar").matches("asdf foobar asdf"));
        assertTrue(new EntireRegexPattern("foo.bar").matches("foo-bar"));
        assertFalse(new EntireRegexPattern("foo.bar").matches("foobar"));
    }
    
    @Test
    public void testEntireString() {
        assertFalse(new EntireStringPattern("foobar").matches("asdf foo-bar asdf"));
        assertFalse(new EntireStringPattern("foobar").matches("asdf foobar asdf"));
        assertFalse(new EntireStringPattern("foobar").matches("foo-bar"));
        assertTrue(new EntireStringPattern("foobar").matches("foobar"));
    }
}
