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
package org.graylog.plugins.views.search.views;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewResolverDecoderTest {
    @Test
    public void testValidResolver() {
        final ViewResolverDecoder decoder = new ViewResolverDecoder("resolver:id");
        assertTrue(decoder.isResolverViewId());
        assertEquals("resolver", decoder.getResolverName());
        assertEquals("id", decoder.getViewId());
    }

    @Test
    public void testStandardView() {
        final ViewResolverDecoder decoder = new ViewResolverDecoder("62068954bd0cd7035876fcec");
        assertFalse(decoder.isResolverViewId());
        assertThatThrownBy(decoder::getResolverName)
                .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(decoder::getViewId)
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
