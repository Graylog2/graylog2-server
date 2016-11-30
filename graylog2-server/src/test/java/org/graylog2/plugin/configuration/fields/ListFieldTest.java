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

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ListFieldTest {

    @Test
    public void testGetFieldType() throws Exception {
        final ListField list = new ListField("list", "The List", Collections.emptyList(), "Hello, this is a list", ConfigurationField.Optional.NOT_OPTIONAL);
        assertThat(list.getFieldType()).isEqualTo(ListField.FIELD_TYPE);
    }

    @Test
    public void testGetDefaultValue() throws Exception {
        final ListField list = new ListField("list", "The List", ImmutableList.of("Foo", "Bar", "Baz"), "Hello, this is a list", ConfigurationField.Optional.NOT_OPTIONAL);
        final Object defaultValue = list.getDefaultValue();
        assertThat(defaultValue instanceof List).isTrue();
        final List<?> defaultValue1 = (List) defaultValue;
        assertThat(defaultValue1.size()).isEqualTo(3);
        assertThat((String) defaultValue1.get(0)).isEqualTo("Foo");
        assertThat((String) defaultValue1.get(1)).isEqualTo("Bar");
        assertThat((String) defaultValue1.get(2)).isEqualTo("Baz");
    }

    @Test
    public void testSetDefaultValue() throws Exception {
        final ListField list = new ListField("list", "The List", Collections.emptyList(), "Hello, this is a list", ConfigurationField.Optional.NOT_OPTIONAL);
        final Object defaultValue1 = list.getDefaultValue();
        assertThat(defaultValue1 instanceof List).isTrue();
        assertThat(((List) defaultValue1).size()).isEqualTo(0);

        list.setDefaultValue(ImmutableList.of("Foo", "Bar"));
        final Object defaultValue2 = list.getDefaultValue();
        assertThat(defaultValue2 instanceof List).isTrue();
        assertThat(((List) defaultValue2).size()).isEqualTo(2);

        list.setDefaultValue("Foo");
        final Object defaultValue3 = list.getDefaultValue();
        assertThat(defaultValue3 instanceof List).isTrue();
        assertThat(((List) defaultValue3).size()).isEqualTo(2);

        list.setDefaultValue(ImmutableList.of(3, "lol"));
        final Object defaultValue4 = list.getDefaultValue();
        assertThat(defaultValue4 instanceof List).isTrue();
        final List defaultValue4List = (List) defaultValue4;
        assertThat(defaultValue4List.size()).isEqualTo(1);
        assertThat(defaultValue4List.get(0)).isEqualTo("lol");
    }

    @Test
    public void testGetValues() throws Exception {
        Map<String,String> values = new HashMap<>();
        values.put("foo", "bar");
        values.put("baz", "lol");

        final ListField list = new ListField("list", "The List", Collections.emptyList(), values, "Hello, this is a list", ConfigurationField.Optional.NOT_OPTIONAL);
        assertThat(list.getAdditionalInformation().get("values")).isEqualTo(values);
    }

    @Test
    public void testGetAttributes() throws Exception {
        final ListField list = new ListField("list", "The List", Collections.emptyList(), "Hello, this is a list", ConfigurationField.Optional.NOT_OPTIONAL);
        assertThat(list.getAttributes().size()).isEqualTo(0);

        final ListField list1 = new ListField("list", "The List", Collections.emptyList(), Collections.emptyMap(), "Hello, this is a list", ConfigurationField.Optional.NOT_OPTIONAL, ListField.Attribute.ALLOW_CREATE);
        assertThat(list1.getAttributes().size()).isEqualTo(1);
        assertThat(list1.getAttributes()).contains("allow_create");
    }

}