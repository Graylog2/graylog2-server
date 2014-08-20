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
package org.graylog2.plugin.configuration.fields;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BooleanFieldTest {

    @Test
    public void testGetFieldType() throws Exception {
        BooleanField f = new BooleanField("name", "Name", false, "description");
        assertEquals(BooleanField.FIELD_TYPE, f.getFieldType());
    }

    @Test
    public void testGetName() throws Exception {
        BooleanField f = new BooleanField("name", "Name", false, "description");
        assertEquals("name", f.getName());
    }

    @Test
    public void testGetHumanName() throws Exception {
        BooleanField f = new BooleanField("name", "Name", false, "description");
        assertEquals("Name", f.getHumanName());
    }

    @Test
    public void testGetDescription() throws Exception {
        BooleanField f = new BooleanField("name", "Name", false, "description");
        assertEquals("description", f.getDescription());
    }

    @Test
    public void testGetDefaultValue() throws Exception {
        BooleanField f = new BooleanField("name", "Name", true, "description");
        assertEquals(true, f.getDefaultValue());
    }

    @Test
    public void testIsOptional() throws Exception {
        BooleanField f = new BooleanField("name", "Name", true, "description");
        assertEquals(ConfigurationField.Optional.OPTIONAL, f.isOptional());
    }

    @Test
    public void testGetAttributes() throws Exception {
        // Boolean field has no attributes.
        BooleanField f = new BooleanField("name", "Name", true, "description");
        assertNotNull(f.getAttributes());
    }

}
