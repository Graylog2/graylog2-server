/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

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