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
package org.graylog2.utilities.date;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NaturalDateParserTest {

    @Test
    public void testParse() throws Exception, NaturalDateParser.DateNotParsableException {
        NaturalDateParser p = new NaturalDateParser();

        NaturalDateParser.Result today = p.parse("today");
        assertNotNull(today.getFrom());
        assertNotNull(today.getTo());

        // It's enough if this does not throw exceptions because we are not testing the underlying library.
        p.parse("today");
        p.parse("last week to today");
    }

    @Test(expectedExceptions = NaturalDateParser.DateNotParsableException.class)
    public void testParseFailsOnUnparsableDate() throws Exception, NaturalDateParser.DateNotParsableException {
        new NaturalDateParser().parse("LOLWUT");
    }

    @Test(expectedExceptions = NaturalDateParser.DateNotParsableException.class)
    public void testParseFailsOnEmptyDate() throws Exception, NaturalDateParser.DateNotParsableException {
        new NaturalDateParser().parse("");
    }

}
