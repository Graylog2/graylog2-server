/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.plugin.configuration.fields;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


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
