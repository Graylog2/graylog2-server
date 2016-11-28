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
package org.graylog2.plugin.configuration.fields;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DropdownFieldTest {

    @Test
    public void testGetFieldType() throws Exception {
        DropdownField f = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(DropdownField.FIELD_TYPE, f.getFieldType());
    }

    @Test
    public void testGetName() throws Exception {
        DropdownField f = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("test", f.getName());
    }

    @Test
    public void testGetHumanName() throws Exception {
        DropdownField f = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("Name", f.getHumanName());
    }

    @Test
    public void testGetDescription() throws Exception {
        DropdownField f = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), "description", ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("description", f.getDescription());
    }

    @Test
    public void testGetDefaultValue() throws Exception {
        DropdownField f = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals("fooval", f.getDefaultValue());
    }

    @Test
    public void testIsOptional() throws Exception {
        DropdownField f = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(ConfigurationField.Optional.NOT_OPTIONAL, f.isOptional());

        DropdownField f2 = new DropdownField("test", "Name", "fooval", new HashMap<String, String>(), ConfigurationField.Optional.OPTIONAL);
        assertEquals(ConfigurationField.Optional.OPTIONAL, f2.isOptional());
    }

    @Test
    public void testGetValues() throws Exception {
        Map<String,String> values = Maps.newHashMap();
        values.put("foo", "bar");
        values.put("zomg", "baz");

        DropdownField f = new DropdownField("test", "Name", "fooval", values, ConfigurationField.Optional.NOT_OPTIONAL);
        assertEquals(values, f.getAdditionalInformation().get("values"));
    }

}
