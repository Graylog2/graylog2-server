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
package org.graylog2.shared.inputs;

import org.graylog2.plugin.IOState;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;

class InputRegistryTest {

    @Test
    void testThreadSafe() {
        // test which was failing pretty reliably back in the days when the input registry was not threadsafe
        final InputRegistry inputRegistry = new InputRegistry();
        IntStream.range(0, 100).parallel().forEach(i -> {
            if (i % 2 == 0) {
                var ignored = inputRegistry.stream().toList();
            } else {
                //noinspection unchecked
                inputRegistry.add(mock((IOState.class)));
            }
        });
    }

}
