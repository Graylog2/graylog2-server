/**
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
 *
 */
package lib;

import org.graylog2.restclient.lib.Version;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class VersionTest {

    @Test
    public void testGetName() throws Exception {
        assertTrue(new Version(0, 20, 0).toString().startsWith("0.20.0"));
        assertTrue(new Version(1, 0, 0, "preview.1").toString().startsWith("1.0.0-preview.1"));
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

}
