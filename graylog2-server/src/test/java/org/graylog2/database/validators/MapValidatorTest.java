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
package org.graylog2.database.validators;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.database.validators.Validator;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapValidatorTest {

    @Test
    public void testValidate() throws Exception {
        Validator v = new MapValidator();

        assertFalse(v.validate(null).passed());
        assertFalse(v.validate(Collections.emptyList()).passed());
        assertFalse(v.validate(9001).passed());
        assertFalse(v.validate("foo").passed());

        Map<String, String> actuallyFilledMap = ImmutableMap.of(
                "foo", "bar",
                "lol", "wut");

        assertTrue(v.validate(actuallyFilledMap).passed());
        assertTrue(v.validate(Collections.emptyMap()).passed());
    }

}
