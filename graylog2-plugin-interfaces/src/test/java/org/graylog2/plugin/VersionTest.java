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
