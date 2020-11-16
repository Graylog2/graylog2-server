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
package org.graylog.plugins.views.search.rest.exceptionmappers;

import org.graylog.plugins.views.search.errors.MissingCapabilitiesException;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.TestData.requirementsMap;


public class MissingCapabilitiesExceptionMapperTest {
    private MissingCapabilitiesExceptionMapper sut;

    @Before
    public void setUp() throws Exception {
        GuiceInjectorHolder.createInjector(Collections.emptyList());

        sut = new MissingCapabilitiesExceptionMapper();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapsValidException() {
        Response response = sut.toResponse(new MissingCapabilitiesException(requirementsMap("one", "two")));

        assertThat(response.getStatus()).isEqualTo(409);

        Map payload = (Map) response.getEntity();
        assertThat(payload).containsOnlyKeys("error", "missing");
        assertThat((String) payload.get("error")).contains("capabilities are missing");
        assertThat((Map) payload.get("missing")).containsOnlyKeys("one", "two");
    }
}
