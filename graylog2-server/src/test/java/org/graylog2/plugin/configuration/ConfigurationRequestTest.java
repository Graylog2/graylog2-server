/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    @Ignore("Throwing CheckReturnValueError")
    @Test
    public void addListField() {
        //Build Configuration Object and expectedValue
        final Map<String, Object> map = new HashMap<>();
        List<String> expectedValue = Arrays.asList("~/ipfix/test.json", "~/ipfix/test1.json");
        map.put("custDefList", expectedValue);
        final Configuration configuration = new Configuration(map);
        //Validate Configuration Object
        assertThat(configuration.getList("custDefList").size()).isEqualTo(2);
        //SetUp ConfigurationRequest
        Map<String, Object> configReqMap = configurationRequest.addListField(Maps.newHashMap(), configuration, "custDefList");
        //Validate ConfigRequest Map
        Stream.of(configReqMap.keySet().toArray())
                .forEach(key -> assertThat(configReqMap.get(key).equals(expectedValue)));
    }
}
