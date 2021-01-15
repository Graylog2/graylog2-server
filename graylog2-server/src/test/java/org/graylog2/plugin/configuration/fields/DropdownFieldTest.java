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
