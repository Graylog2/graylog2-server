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
package org.graylog.plugins.views.search.rest;

import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

class TestableFixedViewResolver implements ViewResolver {

    private final ViewDTO viewDTO;

    public TestableFixedViewResolver(ViewDTO viewDTO) {
        this.viewDTO = viewDTO;
    }

    @Override
    public Optional<ViewDTO> get(String id) {
        return id.equals(viewDTO.id()) ? Optional.of(viewDTO) : Optional.empty();
    }

    @Override
    public Set<String> getSearchIds() {
        return null;
    }

    @Override
    public Set<ViewDTO> getBySearchId(String searchId) {
        return Collections.emptySet();
    }

    @Override
    public boolean canReadView(String viewId, Predicate<String> permissionTester, BiPredicate<String, String> entityPermissionsTester) {
        // Not used in this test.
        return false;
    }


}
