/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionTest {
    @Test
    public void testGetName() throws Exception {
        assertEquals("0.20.0", Version.from(0, 20, 0).toString());
        assertEquals("1.0.0", Version.from(1, 0, 0).toString());
        assertEquals("1.2.3", Version.from(1, 2, 3).toString());
        assertEquals("0.0.7", Version.from(0, 0, 7).toString());
        assertEquals("1.0.0-preview.1", Version.from(1, 0, 0, "preview.1").toString());
        assertEquals("1.0.0-preview.1+deadbeef", Version.from(1, 0, 0, "preview.1", "deadbeef").toString());
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue(Version.from(0, 20, 0).equals(Version.from(0, 20, 0)));
        assertTrue(Version.from(0, 20, 0, "preview.1").equals(Version.from(0, 20, 0, "preview.1")));
        assertTrue(Version.from(1, 2, 3).equals(Version.from(1, 2, 3)));

        Version v = Version.from(0, 20, 0);
        assertEquals(Version.from(0, 20, 0), v);

        assertFalse(Version.from(0, 20, 0).equals(Version.from(0, 20, 1)));
        assertFalse(Version.from(0, 20, 0, "preview.1").equals(Version.from(0, 20, 0, "preview.2")));
        assertFalse(Version.from(0, 20, 0).equals(null));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGreaterMinor() throws Exception {
        Version v = Version.from(0, 20, 0);

        assertTrue(v.greaterMinor(Version.from(0, 19, 0)));
        assertTrue(v.greaterMinor(Version.from(0, 18, 2)));
        assertTrue(v.greaterMinor(Version.from(0, 19, 9001)));

        assertFalse(v.greaterMinor(Version.from(0, 20, 0)));
        assertFalse(v.greaterMinor(Version.from(1, 0, 0)));
        assertFalse(v.greaterMinor(Version.from(1, 0, 9001)));
        assertFalse(v.greaterMinor(Version.from(1, 20, 0)));
        assertFalse(v.greaterMinor(Version.from(1, 1, 0)));
        assertFalse(v.greaterMinor(Version.from(3, 2, 1)));

        assertTrue(v.greaterMinor(Version.from(0, 19, 0, "rc.1")));

        v = Version.from(1, 5, 0);

        assertTrue(v.greaterMinor(Version.from(0, 19, 0)));
        assertTrue(v.greaterMinor(Version.from(1, 0, 0)));
        assertTrue(v.greaterMinor(Version.from(0, 19, 9001)));

        assertFalse(v.greaterMinor(Version.from(1, 6, 0)));
        assertFalse(v.greaterMinor(Version.from(3, 0, 0)));
        assertFalse(v.greaterMinor(Version.from(1, 5, 9001)));
        assertFalse(v.greaterMinor(Version.from(1, 20, 0)));
        assertFalse(v.greaterMinor(Version.from(1, 20, 5)));
        assertFalse(v.greaterMinor(Version.from(3, 2, 1)));

        assertTrue(v.greaterMinor(Version.from(0, 19, 0, "rc.1")));
    }

    @Test
    public void testSameOrHigher() throws Exception {
        Version v = Version.from(0, 20, 2);

        assertTrue(v.sameOrHigher(Version.from(0, 19, 0)));
        assertTrue(v.sameOrHigher(Version.from(0, 18, 2)));
        assertTrue(v.sameOrHigher(Version.from(0, 19, 9001)));

        assertTrue(v.sameOrHigher(Version.from(0, 20, 0)));
        assertFalse(v.sameOrHigher(Version.from(1, 0, 0)));
        assertFalse(v.sameOrHigher(Version.from(1, 0, 9001)));
        assertFalse(v.sameOrHigher(Version.from(1, 20, 0)));
        assertFalse(v.sameOrHigher(Version.from(1, 1, 0)));
        assertFalse(v.sameOrHigher(Version.from(3, 2, 1)));

        assertTrue(v.sameOrHigher(Version.from(0, 19, 0, "rc.1")));
        assertFalse(v.sameOrHigher(Version.from(1, 19, 0, "rc.1")));
        assertFalse(v.sameOrHigher(Version.from(0, 21, 0, "rc.1")));
        assertTrue(v.sameOrHigher(Version.from(0, 20, 1, "rc.1")));
        assertTrue(v.sameOrHigher(Version.from(0, 20, 0, "rc.1")));
        assertTrue(v.sameOrHigher(Version.from(0, 20, 2, "rc.1")));
        assertFalse(v.sameOrHigher(Version.from(0, 20, 3, "rc.1")));

        v = Version.from(1, 5, 0);

        assertTrue(v.sameOrHigher(Version.from(0, 19, 0)));
        assertTrue(v.sameOrHigher(Version.from(1, 0, 0)));
        assertTrue(v.sameOrHigher(Version.from(0, 19, 9001)));
        assertTrue(v.sameOrHigher(Version.from(1, 5, 0)));
        assertTrue(v.sameOrHigher(Version.from(1, 4, 9)));

        assertFalse(v.sameOrHigher(Version.from(1, 6, 0)));
        assertFalse(v.sameOrHigher(Version.from(3, 0, 0)));
        assertFalse(v.sameOrHigher(Version.from(1, 5, 9001)));
        assertFalse(v.sameOrHigher(Version.from(1, 20, 0)));
        assertFalse(v.sameOrHigher(Version.from(1, 20, 5)));
        assertFalse(v.sameOrHigher(Version.from(3, 2, 1)));

        assertTrue(v.sameOrHigher(Version.from(0, 19, 0, "rc.1")));
        assertFalse(v.sameOrHigher(Version.from(2, 19, 0, "rc.1")));
        assertTrue(v.sameOrHigher(Version.from(0, 0, 0)));
        assertFalse(v.sameOrHigher(Version.from(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)));

        // See https://github.com/Graylog2/graylog2-server/issues/2462
        v = Version.from(2, 1, 0, "beta.2");

        assertTrue(v.sameOrHigher(Version.from(2, 1, 0, "alpha.1")));
        assertTrue(v.sameOrHigher(Version.from(2, 1, 0, "beta.1")));
        assertTrue(v.sameOrHigher(Version.from(2, 1, 0, "beta.2")));
        assertTrue(v.sameOrHigher(Version.from(2, 1, 0))); // This needs to work!
        assertFalse(v.sameOrHigher(Version.from(2, 2, 0, "alpha.1")));
        assertFalse(v.sameOrHigher(Version.from(2, 2, 0)));
    }

    @Test
    public void testCompareTo() {
        Version v = Version.from(0, 20, 2);

        assertTrue(v.compareTo(Version.from(0, 19, 0)) > 0);
        assertTrue(v.compareTo(Version.from(0, 18, 2)) > 0);
        assertTrue(v.compareTo(Version.from(0, 19, 9001)) > 0);

        assertTrue(v.compareTo(Version.from(0, 20, 2)) == 0);
        assertTrue(v.compareTo(Version.from(0, 20, 0)) > 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0)) < 0);
        assertTrue(v.compareTo(Version.from(1, 0, 9001)) < 0);
        assertTrue(v.compareTo(Version.from(1, 20, 0)) < 0);
        assertTrue(v.compareTo(Version.from(1, 1, 0)) < 0);
        assertTrue(v.compareTo(Version.from(3, 2, 1)) < 0);

        assertTrue(v.compareTo(Version.from(0, 19, 0, "rc.1")) > 0);
        assertTrue(v.compareTo(Version.from(1, 19, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(Version.from(0, 21, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(Version.from(0, 20, 1, "rc.1")) > 0);
        assertTrue(v.compareTo(Version.from(0, 20, 0, "rc.1")) > 0);
        assertTrue(v.compareTo(Version.from(0, 20, 2, "rc.1")) > 0);
        assertTrue(v.compareTo(Version.from(0, 20, 3, "rc.1")) < 0);

        v = Version.from(1, 5, 0);

        assertTrue(v.compareTo(Version.from(0, 19, 0)) > 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0)) > 0);
        assertTrue(v.compareTo(Version.from(0, 19, 9001)) > 0);
        assertTrue(v.compareTo(Version.from(1, 5, 0)) == 0);
        assertTrue(v.compareTo(Version.from(1, 4, 9)) > 0);

        assertTrue(v.compareTo(Version.from(1, 6, 0)) < 0);
        assertTrue(v.compareTo(Version.from(3, 0, 0)) < 0);
        assertTrue(v.compareTo(Version.from(1, 5, 9001)) < 0);
        assertTrue(v.compareTo(Version.from(1, 20, 0)) < 0);
        assertTrue(v.compareTo(Version.from(1, 20, 5)) < 0);
        assertTrue(v.compareTo(Version.from(3, 2, 1)) < 0);

        assertTrue(v.compareTo(Version.from(0, 19, 0, "rc.1")) > 0);
        assertTrue(v.compareTo(Version.from(2, 19, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(Version.from(0, 0, 0)) > 0);
        assertTrue(v.compareTo(Version.from(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)) < 0);

        v = Version.from(1, 0, 0, "beta.2");
        assertTrue(v.compareTo(Version.from(1, 0, 0, "beta.1")) > 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0, "beta.2")) == 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0, "beta.3")) < 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0, "alpha.1")) > 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0, "alpha.3")) > 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0, "rc.1")) < 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0, "rc.3")) < 0);
        assertTrue(v.compareTo(Version.from(1, 0, 0)) < 0);
    }
}
