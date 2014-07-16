/*
 * Copyright 2012-2014 TORCH GmbH
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
 */
package org.graylog2.plugin;

import org.graylog2.plugin.Version;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class VersionTest {

    @Test
    public void testGetName() throws Exception {
        assertEquals("0.20.0", new Version(0, 20, 0).toString());
        assertEquals("1.0.0", new Version(1, 0, 0).toString());
        assertEquals("1.2.3", new Version(1, 2, 3).toString());
        assertEquals("0.0.7", new Version(0, 0, 7).toString());
        assertEquals("1.0.0-preview.1", new Version(1, 0, 0, "preview.1").toString());
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue(new Version(0, 20, 0).equals(new Version(0, 20, 0)));
        assertTrue(new Version(0, 20, 0, "preview.1").equals(new Version(0, 20, 0, "preview.1")));
        assertTrue(new Version(1, 2, 3).equals(new Version(1, 2, 3)));

        Version v = new Version(0, 20, 0);
        assertTrue(v.equals(v));

        assertFalse(new Version(0, 20, 0).equals(new Version(0, 20, 1)));
        assertFalse(new Version(0, 20, 0, "preview.1").equals(new Version(0, 20, 0, "preview.2")));
        assertFalse(new Version(0, 20, 0).equals(null));
    }

    @Test
    public void testGreaterMinor() throws Exception {
        Version v = new Version(0, 20, 0);

        assertTrue(v.greaterMinor(new Version(0, 19, 0)));
        assertTrue(v.greaterMinor(new Version(0, 18, 2)));
        assertTrue(v.greaterMinor(new Version(0, 19, 9001)));

        assertFalse(v.greaterMinor(new Version(0, 20, 0)));
        assertFalse(v.greaterMinor(new Version(1, 0, 0)));
        assertFalse(v.greaterMinor(new Version(1, 0, 9001)));
        assertFalse(v.greaterMinor(new Version(1, 20, 0)));
        assertFalse(v.greaterMinor(new Version(1, 1, 0)));
        assertFalse(v.greaterMinor(new Version(3, 2, 1)));

        assertTrue(v.greaterMinor(new Version(0, 19, 0, "rc.1")));

        v = new Version(1, 5, 0);

        assertTrue(v.greaterMinor(new Version(0, 19, 0)));
        assertTrue(v.greaterMinor(new Version(1, 0, 0)));
        assertTrue(v.greaterMinor(new Version(0, 19, 9001)));

        assertFalse(v.greaterMinor(new Version(1, 6, 0)));
        assertFalse(v.greaterMinor(new Version(3, 0, 0)));
        assertFalse(v.greaterMinor(new Version(1, 5, 9001)));
        assertFalse(v.greaterMinor(new Version(1, 20, 0)));
        assertFalse(v.greaterMinor(new Version(1, 20, 5)));
        assertFalse(v.greaterMinor(new Version(3, 2, 1)));

        assertTrue(v.greaterMinor(new Version(0, 19, 0, "rc.1")));
    }

}
