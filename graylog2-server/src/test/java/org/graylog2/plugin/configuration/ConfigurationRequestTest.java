/**
 * This file is part of Graylog.
 *
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationRequestTest {
    private ConfigurationRequest configurationRequest;

    @Before
    public void setUp() throws Exception {
        configurationRequest = new ConfigurationRequest();
    }

    @Test
    public void putAllRetainsOrder() throws Exception {
        final ImmutableMap<String, ConfigurationField> fields = ImmutableMap.<String, ConfigurationField>of(
                "field1", new TextField("field1", "humanName", "defaultValue", "description"),
                "field2", new TextField("field2", "humanName", "defaultValue", "description"),
                "field3", new TextField("field3", "humanName", "defaultValue", "description")
        );
        configurationRequest.putAll(fields);

        assertThat(configurationRequest.getFields().keySet()).containsSequence("field1", "field2", "field3");
    }

    @Test
    public void addFieldAppendsFieldAtTheEnd() throws Exception {
        int numberOfFields = 5;
        for (int i = 0; i < numberOfFields; i++) {
            configurationRequest.addField(new TextField("field" + i, "humanName", "defaultValue", "description"));
        }

        assertThat(configurationRequest.getFields().keySet())
                .containsSequence("field0", "field1", "field2", "field3", "field4");
    }

    @Test
    public void asListRetainsOrder() throws Exception {
        int numberOfFields = 5;
        for (int i = 0; i < numberOfFields; i++) {
            configurationRequest.addField(new TextField("field" + i, "humanName", "defaultValue", "description"));
        }

        assertThat(configurationRequest.asList().keySet())
                .containsSequence("field0", "field1", "field2", "field3", "field4");
    }
}
