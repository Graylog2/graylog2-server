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
package org.graylog2.indexer;

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

class GIMMapping6Test extends GIMMappingTest {
    @Test
    void matchesJsonSource() throws Exception {
        final IndexMappingTemplate template = new GIMMapping6();
        final IndexSetConfig indexSetConfig = mockIndexSetConfig();

        final Map<String, Object> result = template.toTemplate(indexSetConfig, "myindex*", -2147483648);

        assertEquals(resource("expected_gim_template6.json"), json(result), true);
    }
}
