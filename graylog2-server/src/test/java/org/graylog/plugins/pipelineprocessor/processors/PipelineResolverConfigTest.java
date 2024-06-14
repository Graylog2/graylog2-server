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
package org.graylog.plugins.pipelineprocessor.processors;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineResolverConfigTest {
    @Test
    void validations() {
        assertThatThrownBy(() -> PipelineResolverConfig.of(null, Stream::of, Stream::of))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PipelineResolverConfig.of(Stream::of, null, Stream::of))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PipelineResolverConfig.of(Stream::of, Stream::of, null))
                .isInstanceOf(NullPointerException.class);
    }
}
