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

package org.graylog2.blacklists;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlacklistCacheTest {

    @Test
    public void testGetInstance() {
        // First call should create instance.
        assertNotNull(BlacklistCache.getInstance());

        // Second call should give back formerly created instance.
        assertNotNull(BlacklistCache.getInstance());
    }

    @Test
    public void testSetAndGet() {
        BlacklistCache.getInstance().set(new ArrayList<Blacklist>());
        BlacklistCache.getInstance().get();
        assertTrue(BlacklistCache.getInstance().valid());
    }



}