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
package org.graylog.plugins.views.search.views;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * View Resolvers provide a way that plugins can provide custom sources for looking up views.
 *
 * Each resolvable view must also have a corresponding {@link org.graylog.plugins.views.search.Search} saved in the
 * database. The ViewResolver methods pertaining to search records must be implemented in order for runtime
 * operation of views to work correctly.
 *
 * See https://github.com/Graylog2/graylog2-server/pull/12280 and
 * https://github.com/Graylog2/graylog2-server/pull/12287 for more information.
 */
public interface ViewResolver {
    Optional<ViewDTO> get(String id);

    /**
     * @param searchId A search id.
     * @return the corresponding {@link ViewDTO} for supplied searchId.
     */
    Set<ViewDTO> getBySearchId(String searchId);

    /**
     * @return A set of all search ids referenced by resolvable views.
     * The search ids must be returned to prevent the searches from being automatically deleted by periodically by
     * {@link org.graylog.plugins.views.search.db.SearchesCleanUpJob}.
     */
    Set<String> getSearchIds();

    /**
     * A method to return whether the current user is authorized to read the current resolved view and its related
     * objects (such as the backing {@link org.graylog.plugins.views.search.Search} record).
     *
     * This method accepts two permissions tester predicates, to allow each implementation to perform any combination
     * of permission checks needed.
     *
     * @param viewId                  The id of the view.
     * @param permissionTester        A predicate, which tests a single string permission parameter.
     *                                For example, is the user authorized to read a view based on having of a
     *                                particular permission?
     * @param entityPermissionsTester A bi-predicate which tests two parameters (the permission to test and the id of
     *                                the entity). For example, is the user authorized to read a view based on having
     *                                a particular permission and/or having access to a particular entity?
     */
    boolean canReadView(String viewId, Predicate<String> permissionTester,
                        BiPredicate<String, String> entityPermissionsTester);
}
