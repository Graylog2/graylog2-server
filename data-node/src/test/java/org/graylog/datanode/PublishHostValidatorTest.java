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
package org.graylog.datanode;

import com.github.joschi.jadconfig.Validator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PublishHostValidatorTest {

    private static final String NAME = "opensearch_network_publish_host";

    private final Validator<String> validator = new PublishHostValidator();

    @Test
    void testValidValues() {
        Assertions.assertThatNoException().isThrownBy(() -> validator.validate(NAME, null));
        Assertions.assertThatNoException().isThrownBy(() -> validator.validate(NAME, ""));
        for (String value : List.of("datanode.example.org", "10.0.0.1", "127.0.0.1", "2001:db8::1", "::1")) {
            Assertions.assertThatNoException().as(value).isThrownBy(() -> validator.validate(NAME, value));
        }
    }

    @Test
    void testWildcardAddressRejected() {
        for (String value : List.of("0.0.0.0", "::", "0:0:0:0:0:0:0:0")) {
            Assertions.assertThatThrownBy(() -> validator.validate(NAME, value))
                    .as(value)
                    .hasMessageContaining("wildcard address");
        }
    }

    @Test
    void testScopedIPv6Rejected() {
        for (String value : List.of("fe80::1%eth0", "fe80::1%1")) {
            Assertions.assertThatThrownBy(() -> validator.validate(NAME, value))
                    .as(value)
                    .hasMessageContaining("zone ID");
        }
    }

    @Test
    void testBracketedIPv6Rejected() {
        for (String value : List.of("[2001:db8::1]", "[::1]")) {
            Assertions.assertThatThrownBy(() -> validator.validate(NAME, value))
                    .as(value)
                    .hasMessageContaining("without brackets");
        }
    }
}
