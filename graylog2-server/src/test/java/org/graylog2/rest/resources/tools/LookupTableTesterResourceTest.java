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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LookupTableTesterResourceTest {
    @Mock
    private LookupTableService lookupTableService;

    @Mock
    private LookupTable table;

    private Subject subject;
    private LookupTableTesterResource resource;
    private final String tableName = "foo-table";
    private final String someKey = "some-key";
    private final String staticResponse = "foo";

    @BeforeEach
    void setUp() {
        when(lookupTableService.getTable(tableName)).thenReturn(table);
        lenient().when(lookupTableService.hasTable(tableName)).thenReturn(true);

        final var tableBuilderMock = mock(LookupTableService.Builder.class);
        lenient().when(tableBuilderMock.lookupTable(tableName)).thenReturn(tableBuilderMock);

        final var lookupTableFunc = mock(LookupTableService.Function.class);
        lenient().when(lookupTableFunc.lookup(anyString())).thenReturn(LookupResult.single(staticResponse));
        lenient().when(tableBuilderMock.build()).thenReturn(lookupTableFunc);

        lenient().when(lookupTableService.newBuilder()).thenReturn(tableBuilderMock);
        when(table.id()).thenReturn(tableName);
        subject = mock(Subject.class);
        this.resource = new LookupTableTesterResource(lookupTableService) {
            @Override
            protected Subject getSubject() {
                return subject;
            }
        };
    }

    @Test
    void rejectTestingLookupTableUserHasNoAccessTo() {
        setUserIsPermittedForLookupTable(false);

        assertThrows(ForbiddenException.class, () -> resource.testLookupTable(tableName, someKey));
        assertThrows(ForbiddenException.class, () -> resource.testLookupTable(LookupTableTestRequest.create(someKey, tableName)));
    }

    @Test
    void performTestingLookupIfUserHasAccessToTable() {
        setUserIsPermittedForLookupTable(true);

        assertThat(resource.testLookupTable(tableName, someKey))
                .satisfies(this::assertSuccessfulResponse);
        assertThat(resource.testLookupTable(LookupTableTestRequest.create(someKey, tableName)))
                .satisfies(this::assertSuccessfulResponse);
    }

    @Test
    void returnsErrorResponseIfUserHasAccessButTableDoesNotExists() {
        setUserIsPermittedForLookupTable(true);
        when(lookupTableService.hasTable(tableName)).thenReturn(false);

        assertThat(resource.testLookupTable(tableName, someKey))
                .satisfies(this::assertErrorResponse);
        assertThat(resource.testLookupTable(LookupTableTestRequest.create(someKey, tableName)))
                .satisfies(this::assertErrorResponse);
    }

    private void setUserIsPermittedForLookupTable(boolean permitted) {
        when(subject.isPermitted(RestPermissions.LOOKUP_TABLES_READ + ":" + tableName)).thenReturn(permitted);
    }

    private void assertSuccessfulResponse(LookupTableTesterResponse response) {
        assertThat(response.error()).isFalse();
        assertThat(response.empty()).isFalse();
        assertThat(response.key()).isEqualTo(someKey);
        assertThat(response.value()).isEqualTo(staticResponse);
    }

    private void assertErrorResponse(LookupTableTesterResponse response) {
        assertThat(response.error()).isTrue();
        assertThat(response.errorMessage()).isEqualTo("Lookup table <" + tableName + "> doesn't exist");
    }
}
