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
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class TextFieldTest {
    @Test
    public void testGetFieldType() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description");
        assertEquals(TextField.FIELD_TYPE, f.getFieldType());
    }

    @Test
    public void testGetName() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description");
        assertEquals("test", f.getName());
    }

    @Test
    public void testGetHumanName() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description");
        assertEquals("Name", f.getHumanName());
    }

    @Test
    public void testGetDescription() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description");
        assertEquals("description", f.getDescription());
    }

    @Test
    public void testGetDefaultValue() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description");
        assertEquals("default", f.getDefaultValue());
    }

    @Test
    public void testIsOptional() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description", ConfigurationField.Optional.OPTIONAL);
        assertEquals(ConfigurationField.Optional.OPTIONAL, f.isOptional());

        TextField f2 = new TextField("test", "Name", "default", "description", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(ConfigurationField.Optional.NOT_OPTIONAL, f2.isOptional());
    }

    @Test
    public void testGetAttributes() throws Exception {
        TextField f = new TextField("test", "Name", "default", "description");
        assertEquals(0, f.getAttributes().size());

        TextField f1 = new TextField("test", "Name", "default", "description", TextField.Attribute.IS_PASSWORD);
        assertEquals(1, f1.getAttributes().size());
        assertTrue(f1.getAttributes().contains("is_password"));
    }
}
