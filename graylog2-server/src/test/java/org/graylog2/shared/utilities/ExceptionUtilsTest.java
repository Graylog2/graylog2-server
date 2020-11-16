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
package org.graylog2.shared.utilities;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionUtilsTest {
    @Test
    public void formatMessageCause() {
        assertThat(ExceptionUtils.formatMessageCause(new Exception())).isNotBlank();
    }
    @Test
    public void getRootCauseMessage() {
        assertThat(ExceptionUtils.getRootCauseMessage(new Exception("cause1", new Exception("root")))).satisfies(m -> {
           assertThat(m).isNotBlank();
           assertThat(m).isEqualTo("root.");
        });
    }
    @Test
    public void getRootCauseOrMessage() {
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1", new Exception("root")))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("root.");
        });
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1"))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("cause1.");
        });
        assertThat(ExceptionUtils.getRootCauseOrMessage(new Exception("cause1", new Exception("")))).satisfies(m -> {
            assertThat(m).isNotBlank();
            assertThat(m).isEqualTo("cause1.");
        });
    }
}
