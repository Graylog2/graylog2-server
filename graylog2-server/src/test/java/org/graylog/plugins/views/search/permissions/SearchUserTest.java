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
package org.graylog.plugins.views.search.permissions;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewLike;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchUserTest {
    public static final String USERNAME = "karl";

    private User mockUser() {
        final User user = mock(User.class);
        when(user.getName()).thenReturn(SearchUserTest.USERNAME);
        return user;
    }

    private SearchUser searchUser() {
        return new SearchUser(mockUser(), (perm) -> true, (perm, id) -> true, Mockito.mock(PermittedStreams.class),
                new HashMap<>());
    }

    @Test
    public void exactUserOfSearchIsOwner() {
        final Search search = Search.builder().owner(USERNAME).build();
        final SearchUser searchUser = searchUser();
        assertThat(searchUser.owns(search)).isTrue();
    }

    @Test
    public void anyUserIsOwnerOfLegacySearchesWithoutOwner() {
        final Search search = Search.builder().build();
        final SearchUser searchUser = searchUser();
        assertThat(searchUser.owns(search)).isTrue();
    }

    @Test
    public void usernameNotMatchingIsNotOwner() {
        final Search search = Search.builder().owner("friedrich").build();
        final SearchUser searchUser = searchUser();
        assertThat(searchUser.owns(search)).isFalse();
    }

    @Test
    void testViewReadAccess() {
        // Verify that all combinations of permission and view ids test successfully.
        assertThat(searchUserRequiringPermission("missing-permission", "bad-id")
                .canReadView(new TestView("do-not-match-id"))).isFalse();
        assertThat(searchUserRequiringPermission(ViewsRestPermissions.VIEW_READ, "bad-id")
                .canReadView(new TestView("do-not-match-id"))).isFalse();
        assertThat(searchUserRequiringPermission("missing-permission", "good-id")
                .canReadView(new TestView("good-id"))).isFalse();
        assertThat(searchUserRequiringPermission(ViewsRestPermissions.VIEW_READ, "good-id")
                .canReadView(new TestView("good-id"))).isTrue();
    }

    private static class TestView implements ViewLike {

        final String id;

        public TestView(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public ViewDTO.Type type() {
            return ViewDTO.Type.DASHBOARD;
        }
    }

    private SearchUser searchUserRequiringPermission(String permission, String id) {
        final HashMap<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put("resolver", new TestViewResolver(null));
        return new SearchUser(mockUser(),
                (perm) -> perm.equals(permission),
                (perm, pId) -> perm.equals(permission) && id.equals(pId),
                Mockito.mock(PermittedStreams.class),
                viewResolvers);
    }

    @Test
    void testResolvedViewReadAccess() {
        // Test that the resolver permission check allows and disallows appropriately.
        assertThat(searchUserResolvedRequiringPermission("missing-permission")
                .canReadView(new TestView("resolver:resolved-id"))).isFalse();
        assertThat(searchUserResolvedRequiringPermission("allowed-permission")
                .canReadView(new TestView("resolver:resolved-id"))).isTrue();

        // Test that the resolver permission and entity id check allows and disallows appropriately.
        assertThat(searchUserResolvedRequiringPermissionEntity("bad-permission", "resolved-id")
                .canReadView(new TestView("resolver:resolved-id"))).isFalse();
        assertThat(searchUserResolvedRequiringPermissionEntity("allowed-permission", "bad-id")
                .canReadView(new TestView("resolver:resolved-id"))).isFalse();
        assertThat(searchUserResolvedRequiringPermissionEntity("missing-permission", "bad-id")
                .canReadView(new TestView("resolver:resolved-id"))).isFalse();
        assertThat(searchUserResolvedRequiringPermissionEntity("allowed-permission", "resolved-id")
                .canReadView(new TestView("resolver:resolved-id"))).isTrue();
    }

    private SearchUser searchUserResolvedRequiringPermission(String expectedPermission) {
        final HashMap<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put("resolver", new TestViewResolver(expectedPermission));
        return new SearchUser(mockUser(),
                (perm) -> perm.equals("allowed-permission"),
                (perm, pId) -> perm.equals("allowed-permission") && "resolved-id".equals(pId),
                Mockito.mock(PermittedStreams.class),
                viewResolvers);
    }

    /**
     * Tests only the single permission check predicate.
     */
    private static class TestViewResolver implements ViewResolver {

        final private String allowPermission;

        public TestViewResolver(String allowPermission) {
            this.allowPermission = allowPermission;
        }

        @Override
        public Optional<ViewDTO> get(String id) {
            return Optional.empty();
        }

        @Override
        public Set<String> getSearchIds() {
            return Collections.emptySet();
        }

        @Override
        public boolean canReadView(String viewId, Predicate<String> permissionTester, BiPredicate<String, String> entityPermissionsTester) {
            return permissionTester.test(allowPermission);
        }

        @Override
        public Set<ViewDTO> getBySearchId(String searchId) {
            return Collections.emptySet();
        }
    }

    private SearchUser searchUserResolvedRequiringPermissionEntity(String expectedPermission, String expectedId) {
        final HashMap<String, ViewResolver> viewResolvers = new HashMap<>();
        viewResolvers.put("resolver", new TestViewResolverEntity(expectedPermission, expectedId));
        return new SearchUser(mockUser(),
                (perm) -> perm.equals("allowed-permission"),
                (perm, pId) -> perm.equals("allowed-permission") && "resolved-id".equals(pId),
                Mockito.mock(PermittedStreams.class),
                viewResolvers);
    }

    /**
     * Tests only the single permission and entity check predicate.
     */
    private static class TestViewResolverEntity implements ViewResolver {

        private final String expectedPermission;
        private final String expectedId;

        public TestViewResolverEntity(String expectedPermission, String expectedId) {
            this.expectedPermission = expectedPermission;
            this.expectedId = expectedId;
        }

        @Override
        public Optional<ViewDTO> get(String id) {
            return Optional.empty();
        }

        @Override
        public Set<String> getSearchIds() {
            return Collections.emptySet();
        }

        @Override
        public boolean canReadView(String viewId, Predicate<String> permissionTester, BiPredicate<String, String> entityPermissionsTester) {
            return entityPermissionsTester.test(expectedPermission, expectedId);
        }

        @Override
        public Set<ViewDTO> getBySearchId(String searchId) {
            return Collections.emptySet();
        }
    }
}
