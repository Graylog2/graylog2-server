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
package org.graylog2.shared.security;

import org.apache.shiro.subject.Subject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShiroPrincipalTest {
    @Test
    public void testGetNameWithNull() throws Exception {
        final Subject subject = mock(Subject.class);
        final ShiroPrincipal shiroPrincipal = new ShiroPrincipal(subject);

        assertThat(shiroPrincipal.getName()).isNull();
    }

    @Test
    public void testGetName() throws Exception {
        final Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn("test");
        final ShiroPrincipal shiroPrincipal = new ShiroPrincipal(subject);

        assertThat(shiroPrincipal.getName()).isEqualTo("test");
    }

    @Test
    public void testGetSubject() throws Exception {
        final Subject subject = mock(Subject.class);
        final ShiroPrincipal shiroPrincipal = new ShiroPrincipal(subject);

        assertThat(shiroPrincipal.getSubject()).isSameAs(subject);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSubject() throws Exception {
        new ShiroPrincipal(null);
    }
}