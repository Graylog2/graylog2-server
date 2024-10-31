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
package org.graylog2.shared.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

import java.util.Optional;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OptionalResponseFilterTest {

    private OptionalResponseFilter toTest;
    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private ContainerResponseContext response;

    @BeforeEach
    void setUp() {
        toTest = new OptionalResponseFilter();
    }


    @Test
    void changesStatusToNoContentForEmptyOptional() {
        doReturn(Optional.empty()).when(response).getEntity();
        toTest.filter(requestContext, response);
        verify(response).setStatus(204);
        verify(response).setEntity(null);
    }

    @Test
    void doesNothingOnPresentOptional() {
        doReturn(Optional.of(new Object())).when(response).getEntity();
        toTest.filter(requestContext, response);
        verifyNoMoreInteractions(response);
    }

    @Test
    void doesNothingOnNoOptional() {
        doReturn(new Object()).when(response).getEntity();
        toTest.filter(requestContext, response);
        verifyNoMoreInteractions(response);
    }

}
