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
package org.graylog2.autovalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class WithBeanGetterTest {
    @Test
    public void testBeanHasJavaBeanGetters() {
        final TestBean bean = TestBean.create("Test", true, false);

        final List<String> methodNames = Arrays.stream(bean.getClass().getMethods())
                .map(Method::getName)
                .collect(Collectors.toList());
        assertThat(methodNames)
                .contains("text", "getText")
                .contains("bool", "isBool")
                .contains("boxedBool", "isBoxedBool");
    }

    @AutoValue
    @WithBeanGetter
    static abstract class TestBean {
        @JsonProperty
        public abstract String text();

        @JsonProperty
        public abstract boolean bool();

        @JsonProperty
        public abstract Boolean boxedBool();

        public static TestBean create(String text, boolean bool, Boolean boxedBool) {
            return new AutoValue_WithBeanGetterTest_TestBean(text, bool, boxedBool);
        }
    }
}
