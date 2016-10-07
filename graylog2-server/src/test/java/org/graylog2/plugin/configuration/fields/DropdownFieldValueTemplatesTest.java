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

import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DropdownFieldValueTemplatesTest {
    private enum TestEnum {
        ONE, TWO
    }

    @Test
    public void testBuildEnumMap() throws Exception {
        final Map<String, String> enumMap = DropdownField.ValueTemplates.valueMapFromEnum(TestEnum.class, (t) -> t.name().toLowerCase(Locale.ENGLISH));
        assertThat(enumMap)
            .containsEntry("ONE", "one")
            .containsEntry("TWO", "two");
    }
}
