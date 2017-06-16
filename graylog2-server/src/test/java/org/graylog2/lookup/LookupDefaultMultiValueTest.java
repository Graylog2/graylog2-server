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

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LookupDefaultMultiValueTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createMulti() throws Exception {
        assertThat(LookupDefaultMultiValue.create("{}", LookupDefaultMultiValue.Type.OBJECT).value())
                .isInstanceOf(Map.class)
                .isEqualTo(Collections.emptyMap());
        assertThat(LookupDefaultMultiValue.create("{\"hello\":\"world\",\"number\":42}", LookupDefaultMultiValue.Type.OBJECT).value())
                .isInstanceOf(Map.class)
                .isEqualTo(ImmutableMap.of("hello", "world", "number", 42));

        assertThat(LookupDefaultMultiValue.create("something", LookupDefaultMultiValue.Type.NULL).value())
                .isNull();
    }

    @Test
    public void createSingleString() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        LookupDefaultMultiValue.create("foo", LookupDefaultMultiValue.Type.STRING);
    }

    @Test
    public void createSingleNumber() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        LookupDefaultMultiValue.create("42", LookupDefaultMultiValue.Type.NUMBER);
    }

    @Test
    public void createSingleBoolean() throws Exception {
        expectedException.expect(IllegalArgumentException.class);

        LookupDefaultMultiValue.create("true", LookupDefaultMultiValue.Type.BOOLEAN);
    }
}