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
package org.graylog.plugins.views.startpage.title;

import org.graylog.grn.GRN;
import org.graylog2.lookup.Catalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class StartPageTitleRetrieverTest {

    @Mock
    private Catalog catalog;

    @Mock
    private GRN grn;

    private StartPageTitleRetriever toTest;

    @BeforeEach
    void setUp() {
        toTest = new StartPageTitleRetriever(catalog);
    }

    @Test
    void testReturnsEmptyOptionalOnEmptyEntryInCatalog() throws Exception {
        doReturn(Optional.empty()).when(catalog).getEntry(any());
        assertTrue(toTest.retrieveTitle(grn).isEmpty());
    }

    @Test
    void testReturnsEmptyOptionalOnExecutionExceptionInCacheLoading() throws Exception {
        doThrow(ExecutionException.class).when(catalog).getEntry(any());
        assertTrue(toTest.retrieveTitle(grn).isEmpty());
    }

    @Test
    void testReturnsIdIfTitleIsMissing() throws Exception {
        doReturn(Optional.of(new Catalog.Entry("id", null))).when(catalog).getEntry(any());
        assertEquals(Optional.of("id"), toTest.retrieveTitle(grn));
    }

    @Test
    void testReturnsTitleIfPresent() throws Exception {
        doReturn(Optional.of(new Catalog.Entry("id", "title"))).when(catalog).getEntry(any());
        assertEquals(Optional.of("title"), toTest.retrieveTitle(grn));
    }
}
