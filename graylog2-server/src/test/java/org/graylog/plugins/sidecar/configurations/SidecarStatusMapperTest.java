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
package org.graylog.plugins.sidecar.configurations;

import org.graylog.plugins.sidecar.mapper.SidecarStatusMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SidecarStatusMapperTest {

    @Test
    public void replaceStringStatusSearchQuery() {
        final SidecarStatusMapper sidecarStatusMapper = new SidecarStatusMapper();

        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:running")).isEqualTo("status:0");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:unknown")).isEqualTo("status:1");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:failing")).isEqualTo("status:2");

        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:failing, status:running")).isEqualTo("status:2, status:0");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("status:failing, foobar")).isEqualTo("status:2, foobar");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("lol:wut, status:failing")).isEqualTo("lol:wut, status:2");
        assertThat(sidecarStatusMapper.replaceStringStatusSearchQuery("lol:wut, status:failing, foobar")).isEqualTo("lol:wut, status:2, foobar");
    }
}