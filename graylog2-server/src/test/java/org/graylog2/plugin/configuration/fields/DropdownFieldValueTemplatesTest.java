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

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DropdownFieldValueTemplatesTest {
    private enum TestEnum {
        ONE, TWO
    }

    private enum TestUTCDefaults {

        ONE(1), NINETY(90);

        private int utcQuickValues;

        TestUTCDefaults(int i){
            utcQuickValues = i;
        }
        public int getValue() { return utcQuickValues; }

    }


    @Test
    public void testBuildEnumMap() throws Exception {
        final Map<String, String> enumMap = DropdownField.ValueTemplates.valueMapFromEnum(TestEnum.class, (t) -> t.name().toLowerCase(Locale.ENGLISH));
        assertThat(enumMap)
                .containsEntry("ONE", "one")
                .containsEntry("TWO", "two");
    }

    @Test
    public void testSinceTimeDefaults() throws Exception {
        Map<String, Integer> enumMap = Arrays.stream(TestUTCDefaults.class.getEnumConstants()).collect(Collectors.toMap(Enum::toString, TestUTCDefaults::getValue));
        assertThat(enumMap)
                .containsEntry("ONE",Integer.valueOf(1))
                .containsEntry("NINETY",Integer.valueOf(90));

    }
}
