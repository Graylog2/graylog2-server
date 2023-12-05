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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog2.lookup.Catalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class StartPageItemTitleRetrieverTest {

    @Mock
    private Catalog catalog;

    @Mock
    private GRN grn;

    @Mock
    private SearchUser searchUser;

    private Map<String, ViewResolver> viewResolvers;

    private StartPageItemTitleRetriever toTest;

    @BeforeEach
    void setUp() {
        viewResolvers = Map.of();
        toTest = new StartPageItemTitleRetriever(catalog, viewResolvers);
    }

    @Test
    void testReturnsEmptyOptionalOnEmptyEntryInCatalog() throws Exception {
        doReturn(Optional.empty()).when(catalog).getEntry(any());
        assertTrue(toTest.retrieveTitle(grn, searchUser).isEmpty());
    }

    @Test
    void testReturnsIdIfTitleIsMissing() throws Exception {
        doReturn(Optional.of(new Catalog.Entry("id", null))).when(catalog).getEntry(any());
        assertEquals(Optional.of("id"), toTest.retrieveTitle(grn, searchUser));
    }

    @Test
    void testReturnsTitleIfPresent() throws Exception {
        doReturn(Optional.of(new Catalog.Entry("id", "title"))).when(catalog).getEntry(any());
        assertEquals(Optional.of("title"), toTest.retrieveTitle(grn, searchUser));
    }
}
