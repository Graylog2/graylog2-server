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
package org.graylog2.indexer.migration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class IndexMigrationProgressTest {
    @Test
    void testProgressPercent() {
        Assertions.assertThat(new IndexMigrationProgress(100, 30, 10, 10).progressPercent()).isEqualTo(50);
        Assertions.assertThat(new IndexMigrationProgress(100, 0, 0, 0).progressPercent()).isEqualTo(0);
        Assertions.assertThat(new IndexMigrationProgress(100, 100, 0, 0).progressPercent()).isEqualTo(100);
        Assertions.assertThat(new IndexMigrationProgress(0, 0, 0, 0).progressPercent()).isEqualTo(100);
    }
}
