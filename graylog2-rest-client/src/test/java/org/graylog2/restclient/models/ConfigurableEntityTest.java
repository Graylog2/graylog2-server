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
package org.graylog2.restclient.models;

import autovalue.shaded.com.google.common.common.collect.Lists;
import autovalue.shaded.com.google.common.common.collect.Maps;
import org.graylog2.restclient.lib.plugin.configuration.RequestedConfigurationField;
import org.graylog2.restclient.lib.plugin.configuration.TextField;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class ConfigurableEntityTest {
    @Mock private ConfigurableEntity configurableEntity;
    private final Map<String, Object> configuration = Maps.newHashMap();
    private final List<RequestedConfigurationField> requestedConfigurationFields = Lists.newArrayList();

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(configurableEntity.getConfiguration(any(List.class))).thenCallRealMethod();
        when(configurableEntity.getConfiguration()).thenReturn(configuration);
    }

    @Test
    public void testMaskingPasswordField() throws Exception {
        final String passwordField = "password";
        configuration.put(passwordField, "foobar");
        addPasswordField(passwordField);

        final Map<String, Object> result = configurableEntity.getConfiguration(requestedConfigurationFields);

        assertEquals(result.get(passwordField), "*******", "Password should be masked in result!");
    }

    @Test
    public void testNotStubbingNonPasswordField() throws Exception {
        final String userField = "username";
        final String username = "johndoe";
        configuration.put(userField, username);
        addNonPasswordField(userField);

        final Map<String, Object> result = configurableEntity.getConfiguration(requestedConfigurationFields);

        assertEquals(result.get(userField), username, "Non-password field should not be masked in result!");
    }

    @Test
    public void testMixedPasswordAndNonPasswordFields() throws Exception {
        final String passwordField = "password";
        configuration.put(passwordField, "foobar");
        addPasswordField(passwordField);

        final String userField = "username";
        final String username = "johndoe";
        configuration.put(userField, username);
        addNonPasswordField(userField);

        final Map<String, Object> result = configurableEntity.getConfiguration(requestedConfigurationFields);

        assertEquals(result.get(userField), username, "Non-password field should not be masked in result!");
        assertEquals(result.get(passwordField), "*******", "Password should be masked in result!");
    }

    private void addNonPasswordField(String userField) {
        final Map.Entry<String, Map<String, Object>> textField = stubConfigurationFieldConfig("text", userField, new ArrayList<String>(), false);

        requestedConfigurationFields.add(new TextField(textField));
    }

    private void addPasswordField(String passwordFieldName) {
        final Map.Entry<String, Map<String, Object>> passwordTextField = stubConfigurationFieldConfig("text", passwordFieldName, new ArrayList<String>() {{
            add("is_password");
        }}, false);

        requestedConfigurationFields.add(new TextField(passwordTextField));
    }

    private Map.Entry<String, Map<String, Object>> stubConfigurationFieldConfig(final String type, final String title, final List<String> attributes, final Boolean isOptional) {
        return new Map.Entry<String, Map<String, Object>>() {
                @Override
                public String getKey() {
                    return title;
                }

                @Override
                public Map<String, Object> getValue() {
                    return new HashMap<String, Object>() {{
                        put("type", type);
                        put("attributes", attributes);
                        put("is_optional", isOptional);
                    }};
                }

                @Override
                public Map<String, Object> setValue(Map<String, Object> value) {
                    return null;
                }
            };
    }
}