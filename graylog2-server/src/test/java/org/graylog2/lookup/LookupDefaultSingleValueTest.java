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
package org.graylog2.lookup;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupDefaultSingleValueTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createSingle() throws Exception {
        assertThat(LookupDefaultSingleValue.create("foo", LookupDefaultSingleValue.Type.STRING).value())
                .isEqualTo("foo");
        assertThat(LookupDefaultSingleValue.create("123", LookupDefaultSingleValue.Type.STRING).value())
                .isEqualTo("123");

        assertThat(LookupDefaultSingleValue.create("42", LookupDefaultSingleValue.Type.NUMBER).value())
                .isInstanceOf(Integer.class)
                .isEqualTo(42);
        assertThat(LookupDefaultSingleValue.create("42.1", LookupDefaultSingleValue.Type.NUMBER).value())
                .isInstanceOf(Double.class)
                .isEqualTo(42.1D);
        assertThat(LookupDefaultSingleValue.create(String.valueOf(Integer.MAX_VALUE + 10L), LookupDefaultSingleValue.Type.NUMBER).value())
                .isInstanceOf(Long.class)
                .isEqualTo(2147483657L);

        assertThat(LookupDefaultSingleValue.create("true", LookupDefaultSingleValue.Type.BOOLEAN).value())
                .isInstanceOf(Boolean.class)
                .isEqualTo(true);
        assertThat(LookupDefaultSingleValue.create("false", LookupDefaultSingleValue.Type.BOOLEAN).value())
                .isInstanceOf(Boolean.class)
                .isEqualTo(false);

        assertThat(LookupDefaultSingleValue.create("something", LookupDefaultSingleValue.Type.NULL).value())
                .isNull();
    }

    @Test
    public void createMultiObject() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        LookupDefaultSingleValue.create("{\"hello\":\"world\",\"number\":42}", LookupDefaultSingleValue.Type.OBJECT);
    }
}