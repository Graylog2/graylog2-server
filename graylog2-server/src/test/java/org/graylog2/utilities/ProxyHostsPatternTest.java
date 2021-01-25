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

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ProxyHostsPatternTest {
    private AbstractBooleanAssert assertPattern(String pattern, String hostOrIp) {
        return assertThat(ProxyHostsPattern.create(pattern).matches(hostOrIp));
    }

    @Test
    public void matches() {
        assertPattern(null, "127.0.0.1").isFalse();
        assertPattern("", "127.0.0.1").isFalse();
        assertPattern(",,", "127.0.0.1").isFalse();

        assertPattern("127.0.0.1", "127.0.0.1").isTrue();
        assertPattern("127.0.0.1", "127.0.0.2").isFalse();
        assertPattern("127.0.0.*", "127.0.0.1").isTrue();
        assertPattern("127.0.*", "127.0.0.1").isTrue();
        assertPattern("127.0.*,10.0.0.*", "127.0.0.1").isTrue();

        assertPattern("node0.graylog.example.com", "node0.graylog.example.com").isTrue();
        assertPattern("node0.graylog.example.com", "node1.graylog.example.com").isFalse();
        assertPattern("*.graylog.example.com", "node0.graylog.example.com").isTrue();
        assertPattern("*.graylog.example.com", "node1.graylog.example.com").isTrue();
        assertPattern("node0.graylog.example.*", "node0.GRAYLOG.example.com").isTrue();
        assertPattern("node0.graylog.example.*,127.0.0.1,*.graylog.example.com", "node1.graylog.example.com").isTrue();

        // Wildcard is only supported at beginning or end of the pattern
        assertPattern("127.0.*.1", "127.0.0.1").isFalse();
        assertPattern("node0.*.example.com", "node0.graylog.example.com").isFalse();
        assertPattern("*.0.0.*", "127.0.0.1").isFalse();
    }
}