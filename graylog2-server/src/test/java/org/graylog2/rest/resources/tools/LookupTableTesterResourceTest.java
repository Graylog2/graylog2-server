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
package org.graylog2.rest.resources.tools;

import jakarta.ws.rs.ForbiddenException;
import org.apache.shiro.subject.Subject;
import org.graylog2.lookup.LookupTable;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.rest.models.tools.requests.LookupTableTestRequest;
import org.graylog2.rest.resources.tools.responses.LookupTableTesterResponse;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LookupTableTesterResourceTest {
    private static final String TABLE_NAME = "secret-table";
    private static final String TABLE_ID = "54e3deadbeefdeadbeef0001";
    private static final String OTHER_TABLE_ID = "54e3deadbeefdeadbeef0002";

    private LookupTableService lookupTableService;
    private Subject subject;
    private LookupTableTesterResource resource;

    @BeforeEach
    void setUp() {
        lookupTableService = mock(LookupTableService.class);
        subject = mock(Subject.class);
        resource = new LookupTableTesterResource(lookupTableService) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    @Test
    void getRejectsUserWithoutReadPermissionForTable() {
        final LookupTable table = existingTable();

        assertThatThrownBy(() -> resource.grokTest(TABLE_NAME, "foo"))
                .isInstanceOf(ForbiddenException.class);

        verify(table, never()).lookup(any());
    }

    @Test
    void postRejectsUserWithoutReadPermissionForTable() {
        final LookupTable table = existingTable();

        assertThatThrownBy(() -> resource.testLookupTable(LookupTableTestRequest.create("foo", TABLE_NAME)))
                .isInstanceOf(ForbiddenException.class);

        verify(table, never()).lookup(any());
    }

    @Test
    void rejectsUserWithReadPermissionForDifferentTable() {
        existingTable();
        grantReadPermissionFor(OTHER_TABLE_ID);

        assertThatThrownBy(() -> resource.grokTest(TABLE_NAME, "foo"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsUserWithoutInstanceScopedReadPermission() {
        existingTable();
        when(subject.isPermitted(RestPermissions.LOOKUP_TABLES_READ)).thenReturn(true);

        assertThatThrownBy(() -> resource.grokTest(TABLE_NAME, "foo"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getReturnsLookupResultForUserWithReadPermissionForTable() {
        final LookupTable table = existingTable();
        when(table.lookup("foo")).thenReturn(LookupResult.single("bar"));
        grantReadPermissionFor(TABLE_ID);

        final LookupTableTesterResponse response = resource.grokTest(TABLE_NAME, " foo ");

        assertThat(response.error()).isFalse();
        assertThat(response.empty()).isFalse();
        assertThat(response.key()).isEqualTo(" foo ");
        assertThat(response.value()).isEqualTo("bar");
    }

    @Test
    void postReturnsLookupResultForUserWithReadPermissionForTable() {
        final LookupTable table = existingTable();
        when(table.lookup("foo")).thenReturn(LookupResult.single("bar"));
        grantReadPermissionFor(TABLE_ID);

        final LookupTableTesterResponse response =
                resource.testLookupTable(LookupTableTestRequest.create("foo", TABLE_NAME));

        assertThat(response.error()).isFalse();
        assertThat(response.empty()).isFalse();
        assertThat(response.key()).isEqualTo("foo");
        assertThat(response.value()).isEqualTo("bar");
    }

    @Test
    void returnsEmptyResultWhenLookupHasNoValue() {
        final LookupTable table = existingTable();
        when(table.lookup("foo")).thenReturn(LookupResult.empty());
        grantReadPermissionFor(TABLE_ID);

        final LookupTableTesterResponse response = resource.grokTest(TABLE_NAME, "foo");

        assertThat(response.error()).isFalse();
        assertThat(response.empty()).isTrue();
        assertThat(response.value()).isNull();
    }

    private LookupTable existingTable() {
        final LookupTable table = mock(LookupTable.class);
        when(table.id()).thenReturn(TABLE_ID);
        when(lookupTableService.getTable(TABLE_NAME)).thenReturn(table);
        when(lookupTableService.hasTable(TABLE_NAME)).thenReturn(true);
        when(lookupTableService.newBuilder()).thenReturn(new LookupTableService.Builder(lookupTableService));
        return table;
    }

    private void grantReadPermissionFor(String tableId) {
        when(subject.isPermitted(RestPermissions.LOOKUP_TABLES_READ + ":" + tableId)).thenReturn(true);
    }
}
