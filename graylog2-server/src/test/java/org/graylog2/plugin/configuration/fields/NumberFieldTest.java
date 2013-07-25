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
import static org.junit.Assert.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NumberFieldTest {

    @Test
    public void testGetFieldType() throws Exception {
        NumberField f = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(NumberField.FIELD_TYPE, f.getFieldType());
    }

    @Test
    public void testGetName() throws Exception {
        NumberField f = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("test", f.getName());
    }

    @Test
    public void testGetHumanName() throws Exception {
        NumberField f = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("Name", f.getHumanName());
    }

    @Test
    public void testGetDescription() throws Exception {
        NumberField f = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("foo", f.getDescription());
    }

    @Test
    public void testGetDefaultValue() throws Exception {
        NumberField f = new NumberField("test", "Name", 9001, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(9001, f.getDefaultValue());
    }

    @Test
    public void testIsOptional() throws Exception {
        NumberField f = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(ConfigurationField.Optional.NOT_OPTIONAL, f.isOptional());

        NumberField f2 = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.OPTIONAL);
        assertEquals(ConfigurationField.Optional.OPTIONAL, f2.isOptional());
    }

    @Test
    public void testGetAttributes() throws Exception {
        NumberField f = new NumberField("test", "Name", 0, "foo", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(0, f.getAttributes().size());

        NumberField f1 = new NumberField("test", "Name", 0, "foo", NumberField.Attribute.IS_PORT_NUMBER);
        assertEquals(1, f1.getAttributes().size());
        assertTrue(f1.getAttributes().contains("is_port_number"));

        NumberField f2 = new NumberField("test", "Name", 0, "foo", NumberField.Attribute.IS_PORT_NUMBER, NumberField.Attribute.ONLY_POSITIVE);
        assertEquals(2, f2.getAttributes().size());
        assertTrue(f2.getAttributes().contains("is_port_number"));
        assertTrue(f2.getAttributes().contains("only_positive"));
    }

}
