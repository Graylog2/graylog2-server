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
package org.graylog2.plugin;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ToolsTest {

    @Test
    public void testGetUriWithPort() throws Exception {
        final URI uriWithPort = new URI("http://example.com:12345");
        final URI uriWithoutPort = new URI("http://example.com");

        assertEquals(Tools.getUriWithPort(uriWithPort, 1).getPort(), 12345);
        assertEquals(Tools.getUriWithPort(uriWithoutPort, 1).getPort(), 1);
    }

    @Test
    public void testGetUriWithScheme() throws Exception {
        assertEquals(Tools.getUriWithScheme(new URI("http://example.com"), "gopher").getScheme(), "gopher");
        assertNull(Tools.getUriWithScheme(new URI("http://example.com"), null).getScheme());
    }
}
