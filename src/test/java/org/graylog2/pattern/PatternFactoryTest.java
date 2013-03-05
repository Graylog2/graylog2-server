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

public class PatternFactoryTest {
    private final char[] REGEX_CHARS = new char[] {'[', '.', '^', '$', '?', '*', '+', '{', '|', '(', '\\'};
    private final String[] REGEX_PATTERNS = new String[] {
            "foo[bar]",
            "foo.bar",
            "^foobar",
            "foobar$",
            "foo?bar",
            "fo*bar",
            "fo{1,2}bar",
            "foo|bar",
            "(foobar)",
            "foo\\sbar"
    };
    
    @Test
    public void testPartialRegex() {
        for(String string : REGEX_PATTERNS) {
            Pattern pattern = PatternFactory.matchPartially(string);
            assertTrue(pattern instanceof PartialRegexPattern);
        }
    }
    
    @Test
    public void testPartialString() {
        for(char c : REGEX_CHARS) {
            String string = "foo\\" + c + "bar";
            Pattern pattern = PatternFactory.matchPartially(string);
            assertTrue(pattern instanceof PartialStringPattern);
        }
    }
    
    @Test
    public void testEntireRegex() {
        for(String string : REGEX_PATTERNS) {
            Pattern pattern = PatternFactory.matchEntirely(string);
            assertTrue(pattern instanceof EntireRegexPattern);
        }
    }

    @Test
    public void testEntireString() {
        for(char c : REGEX_CHARS) {
            String string = "foo\\" + c + "bar";
            Pattern pattern = PatternFactory.matchEntirely(string);
            assertTrue(pattern instanceof EntireStringPattern);
        }
    }
    
    @Test
    public void testPromotionToPartial() {
        assertTrue((PatternFactory.matchEntirely(".*foobar.*")) instanceof PartialStringPattern);
        assertTrue((PatternFactory.matchEntirely("^.*foobar.*$")) instanceof PartialStringPattern);
        assertTrue((PatternFactory.matchEntirely(".*foo.bar.*")) instanceof PartialRegexPattern);
        assertTrue((PatternFactory.matchEntirely("^.*foo.bar.*$")) instanceof PartialRegexPattern);
    }
}
