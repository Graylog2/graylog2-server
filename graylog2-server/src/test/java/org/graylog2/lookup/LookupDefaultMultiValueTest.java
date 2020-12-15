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