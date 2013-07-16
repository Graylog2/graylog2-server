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
package org.graylog2.database.validators;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MapValidatorTest {

    @Test
    public void testValidate() throws Exception {
        Validator v = new MapValidator();

        assertFalse(v.validate(null));
        assertFalse(v.validate(new LinkedList<Integer>()));
        assertFalse(v.validate(9001));
        assertFalse(v.validate("foo"));

        Map<String, String> actuallyFilledMap = new TreeMap<String, String>();
        actuallyFilledMap.put("foo", "bar");
        actuallyFilledMap.put("lol", "wut");

        assertTrue(v.validate(actuallyFilledMap));
        assertTrue(v.validate(new HashMap<String, String>()));
    }

}
