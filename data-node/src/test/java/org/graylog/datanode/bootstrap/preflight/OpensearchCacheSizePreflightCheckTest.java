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
package org.graylog.datanode.bootstrap.preflight;

import org.assertj.core.api.Assertions;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

class OpensearchCacheSizePreflightCheckTest {

    @Test
    void testIllegalCacheSizeValue(@TempDir Path temp) {
        final OpensearchCacheSizePreflightCheck check = new OpensearchCacheSizePreflightCheck("10g", temp, path -> gbToBytes(5));
        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("Unexpected value 10g of node_search_cache_size");
    }

    @Test
    void testCacheSizeErrors(@TempDir Path temp) {
        final OpensearchCacheSizePreflightCheck check = new OpensearchCacheSizePreflightCheck("10gb", temp, path -> gbToBytes(5));
        Assertions.assertThatThrownBy(check::runCheck)
                .isInstanceOf(PreflightCheckException.class)
                .hasMessageContaining("There is not enough usable space for the node search cache");


        Assertions.assertThatNoException().isThrownBy(() -> new OpensearchCacheSizePreflightCheck("2gb", temp, path -> gbToBytes(15)).runCheck());
    }

    private static long gbToBytes(long gb) {
        return gb * 1024 * 1024 * 1024;
    }
}
