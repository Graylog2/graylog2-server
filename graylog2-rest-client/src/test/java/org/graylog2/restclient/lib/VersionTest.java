/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.lib;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

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
    public void testCompareTo() {
        Version v = new Version(0, 20, 2);

        assertTrue(v.compareTo(new Version(0, 19, 0)) > 0);
        assertTrue(v.compareTo(new Version(0, 18, 2)) > 0);
        assertTrue(v.compareTo(new Version(0, 19, 9001)) > 0);

        assertTrue(v.compareTo(new Version(0, 20, 2)) == 0);
        assertTrue(v.compareTo(new Version(0, 20, 0)) > 0);
        assertTrue(v.compareTo(new Version(1, 0, 0)) < 0);
        assertTrue(v.compareTo(new Version(1, 0, 9001)) < 0);
        assertTrue(v.compareTo(new Version(1, 20, 0)) < 0);
        assertTrue(v.compareTo(new Version(1, 1, 0)) < 0);
        assertTrue(v.compareTo(new Version(3, 2, 1)) < 0);

        assertTrue(v.compareTo(new Version(0, 19, 0, "rc.1")) > 0);
        assertTrue(v.compareTo(new Version(1, 19, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(new Version(0, 21, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(new Version(0, 20, 1, "rc.1")) > 0);
        assertTrue(v.compareTo(new Version(0, 20, 0, "rc.1")) > 0);
        assertTrue(v.compareTo(new Version(0, 20, 2, "rc.1")) > 0);
        assertTrue(v.compareTo(new Version(0, 20, 3, "rc.1")) < 0);

        v = new Version(1, 5, 0);

        assertTrue(v.compareTo(new Version(0, 19, 0)) > 0);
        assertTrue(v.compareTo(new Version(1, 0, 0)) > 0);
        assertTrue(v.compareTo(new Version(0, 19, 9001)) > 0);
        assertTrue(v.compareTo(new Version(1, 5, 0)) == 0);
        assertTrue(v.compareTo(new Version(1, 4, 9)) > 0);

        assertTrue(v.compareTo(new Version(1, 6, 0)) < 0);
        assertTrue(v.compareTo(new Version(3, 0, 0)) < 0);
        assertTrue(v.compareTo(new Version(1, 5, 9001)) < 0);
        assertTrue(v.compareTo(new Version(1, 20, 0)) < 0);
        assertTrue(v.compareTo(new Version(1, 20, 5)) < 0);
        assertTrue(v.compareTo(new Version(3, 2, 1)) < 0);

        assertTrue(v.compareTo(new Version(0, 19, 0, "rc.1")) > 0);
        assertTrue(v.compareTo(new Version(2, 19, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(new Version(0, 0, 0)) > 0);
        assertTrue(v.compareTo(new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)) < 0);

        v = new Version(1, 0, 0, "beta.2");
        assertTrue(v.compareTo(new Version(1, 0, 0, "beta.1")) > 0);
        assertTrue(v.compareTo(new Version(1, 0, 0, "beta.2")) == 0);
        assertTrue(v.compareTo(new Version(1, 0, 0, "beta.3")) < 0);
        assertTrue(v.compareTo(new Version(1, 0, 0, "alpha.1")) > 0);
        assertTrue(v.compareTo(new Version(1, 0, 0, "alpha.3")) > 0);
        assertTrue(v.compareTo(new Version(1, 0, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(new Version(1, 0, 0, "rc.3")) < 0);
        assertTrue(v.compareTo(new Version(1, 0, 0)) < 0);
    }
}
