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
package org.graylog2.shared.rest.resources;

import org.graylog2.shared.rest.resources.ProxiedResource.MasterResponse;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;


public class ProxiedResourceTest {
    @Test
    public void masterResponse() {
        final MasterResponse<String> response1 = MasterResponse.create(true, 200, "hello world", null);

        assertThat(response1.isSuccess()).isTrue();
        assertThat(response1.code()).isEqualTo(200);
        assertThat(response1.entity()).get().isEqualTo("hello world");
        assertThat(response1.error()).isNotPresent();
        assertThat(response1.body()).isEqualTo("hello world");

        final MasterResponse<String> response2 = MasterResponse.create(false, 400, null, "error".getBytes(UTF_8));

        assertThat(response2.isSuccess()).isFalse();
        assertThat(response2.code()).isEqualTo(400);
        assertThat(response2.entity()).isNotPresent();
        assertThat(response2.error()).get().isEqualTo("error".getBytes(UTF_8));
        assertThat(response2.body()).isEqualTo("error".getBytes(UTF_8));
    }
}