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
package org.graylog2.utilities;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.AbstractConfigurationField;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.database.ValidationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class ConfigurationMapConverterTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConvertValues() throws Exception {
        final ImmutableMap<String, String> dropdownChoices = ImmutableMap.of(
                "a", "1",
                "b", "2");
        final ConfigurationRequest cr = new ConfigurationRequest();
        cr.addField(new TextField("string", "string", "default", ""));
        cr.addField(new TextField("empty-string", "empty", "", ""));
        cr.addField(new TextField("null-string", "null", null, ""));
        cr.addField(new TextField("non-string", "non-string", null, ""));
        cr.addField(new NumberField("number", "number", 42, ""));
        cr.addField(new BooleanField("boolean-true", "true", false, ""));
        cr.addField(new BooleanField("boolean-false", "false", false, ""));
        cr.addField(new DropdownField("dropdown", "dropdown", "a", dropdownChoices, "", ConfigurationField.Optional.NOT_OPTIONAL));
        cr.addField(new DropdownField("dropdown-empty", "dropdown-empty", "", dropdownChoices, "", ConfigurationField.Optional.NOT_OPTIONAL));
        cr.addField(new DropdownField("dropdown-null", "dropdown-null", "", dropdownChoices, "", ConfigurationField.Optional.NOT_OPTIONAL));

        final UUID uuid = UUID.randomUUID();
        final Map<String, Object> data = new HashMap<>();
        data.put("string", "foo");
        data.put("empty-string", "");
        data.put("null-string", null);
        data.put("non-string", uuid);
        data.put("number", "5");
        data.put("boolean-true", "true");
        data.put("boolean-false", "false");
        data.put("dropdown", "a");
        data.put("dropdown-empty", "");
        data.put("dropdown-null", null);

        final Map<String, Object> config = ConfigurationMapConverter.convertValues(data, cr);
        assertThat(config).contains(
                entry("string", "foo"),
                entry("empty-string", ""),
                entry("null-string", ""),
                entry("non-string", uuid.toString()),
                entry("number", 5),
                entry("boolean-true", true),
                entry("boolean-false", false),
                entry("dropdown", "a"),
                entry("dropdown-empty", ""),
                entry("dropdown-null", "")
        );
    }

    @Test
    public void convertValuesThrowsIllegalArgumentExceptionOnEmptyFieldDescription() throws Exception {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Unknown configuration field description for field \"string\"");

        final ConfigurationRequest cr = new ConfigurationRequest();

        final Map<String, Object> data = new HashMap<>();
        data.put("string", "foo");

        ConfigurationMapConverter.convertValues(data, cr);
    }

    @Test
    public void convertValuesThrowsIllegalArgumentExceptionOnUnknwonType() throws Exception {
        thrown.expect(ValidationException.class);
        thrown.expectMessage("Unknown configuration field type \"dummy\"");

        final ConfigurationRequest cr = new ConfigurationRequest();
        cr.addField(new DummyField());

        final Map<String, Object> data = new HashMap<>();
        data.put("dummy", "foo");

        ConfigurationMapConverter.convertValues(data, cr);
    }

    public static class DummyField extends AbstractConfigurationField {
        public DummyField() {
            super("dummy", "dummy", "humanName", "description", Optional.NOT_OPTIONAL);
        }

        @Override
        public Object getDefaultValue() {
            return "";
        }

        @Override
        public void setDefaultValue(Object defaultValue) {
        }
    }
}