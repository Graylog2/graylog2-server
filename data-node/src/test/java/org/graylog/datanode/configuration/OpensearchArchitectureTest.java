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
package org.graylog.datanode.configuration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class OpensearchArchitectureTest {

    @Test
    void nameKnown() {
        final OpensearchArchitecture amd64 = OpensearchArchitecture.fromCode("amd64");
        final OpensearchArchitecture x8664 = OpensearchArchitecture.fromCode("x86_64");
        final OpensearchArchitecture x64 = OpensearchArchitecture.fromCode("x64");
        Assertions.assertThat(amd64).isEqualTo(OpensearchArchitecture.x64);
        Assertions.assertThat(x8664).isEqualTo(OpensearchArchitecture.x64);
        Assertions.assertThat(x64).isEqualTo(OpensearchArchitecture.x64);
    }

    @Test
    void fromCodeUnknown() {
        Assertions.assertThatThrownBy(() -> OpensearchArchitecture.fromCode("nonsense"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Unsupported OpenSearch distribution architecture: nonsense");
    }
}
