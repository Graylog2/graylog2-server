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

package org.graylog2.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReservedIpCheckerTest {

    @Test
    void testIsEmpty() {

        ReservedIpChecker checker = ReservedIpChecker.getInstance();
        Assertions.assertFalse(checker.isEmpty(), "Reserved IP Blocks expected to be non-empty");

    }

    @Test
    void testIsReservedIpAddress() {

        Assertions.assertTrue(ReservedIpChecker.getInstance().isReservedIpAddress("127.0.0.1"));
        Assertions.assertTrue(ReservedIpChecker.getInstance().isReservedIpAddress("192.168.1.10"));
        Assertions.assertFalse(ReservedIpChecker.getInstance().isReservedIpAddress("104.44.23.89"));
    }

}
