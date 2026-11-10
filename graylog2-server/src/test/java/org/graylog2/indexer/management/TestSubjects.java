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
package org.graylog2.indexer.management;

import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.Subject;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A Shiro subject holding exactly the given permissions, matched with Shiro's own wildcard semantics as in
 * Graylog: {@code indices:changestate} covers every index, {@code indices:changestate:graylog_3} only that one.
 */
public final class TestSubjects {
    private TestSubjects() {
    }

    public static Subject withPermissions(String... granted) {
        final List<WildcardPermission> permissions = Arrays.stream(granted).map(WildcardPermission::new).toList();
        final Subject subject = mock(Subject.class);
        when(subject.isPermitted(anyString())).thenAnswer(invocation -> {
            final WildcardPermission asked = new WildcardPermission(invocation.getArgument(0, String.class));
            return permissions.stream().anyMatch(permission -> permission.implies(asked));
        });
        when(subject.getPrincipal()).thenReturn("test-user");
        return subject;
    }

    public static Subject admin() {
        return withPermissions("*");
    }
}
