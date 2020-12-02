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

import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DateValidatorTest {

    @Test
    public void testValidate() throws Exception {
        Validator v = new DateValidator();

        assertFalse(v.validate(null).passed());
        assertFalse(v.validate(9001).passed());
        assertFalse(v.validate("").passed());
        assertFalse(v.validate(new java.util.Date()).passed());

        // Only joda datetime.
        assertTrue(v.validate(new org.joda.time.DateTime(DateTimeZone.UTC)).passed());

        // Only accepts UTC.
        assertFalse(v.validate(new org.joda.time.DateTime(DateTimeZone.forID("+09:00"))).passed());
    }
}
