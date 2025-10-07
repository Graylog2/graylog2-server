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
package org.graylog.mcp.server;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.grn.GRNType;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class McpRestResourceTest {

    private static InMemoryClusterConfigService clusterConfigService;
    private static McpRestResource resource;

    @BeforeAll
    public static void setUp() throws Exception {
        clusterConfigService = new InMemoryClusterConfigService();
        final Injector injector = GuiceInjectorHolder.createInjector(List.of(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ClusterConfigService.class).toInstance(clusterConfigService);
                        bind(SecurityContext.class).toInstance(new DummySecurityContext());
                        bind(UserService.class).toInstance(new DummyUserService());
                        bind(AuditEventSender.class).toInstance(new NullAuditEventSender());
                        MapBinder.newMapBinder(binder(), TypeLiteral.get(String.class),
                                               new TypeLiteral<Tool<?, ?>>() {});
                        MapBinder.newMapBinder(binder(), GRNType.class, ResourceProvider.class);
                    }
                }
        ));
        resource = injector.getInstance(McpRestResource.class);
        clusterConfigService.write(McpConfiguration.create(false));
    }

    @Test
    public void postDisabledWithClusterConfig() {
        final Response response = resource.get();
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void getDisabledWithClusterConfig() {
        final Response response = resource.get();
        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    private static class DummySecurityContext implements SecurityContext {
        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public boolean isUserInRole(final String role) {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return "";
        }
    }

    private static class DummyUserService implements UserService {
        @Nullable
        @Override
        public User load(final String username) {
            return null;
        }

        @Override
        public List<User> loadAllByName(final String username) {
            return List.of();
        }

        @Nullable
        @Override
        public User loadById(final String id) {
            return null;
        }

        @Override
        public List<User> loadByIds(final Collection<String> ids) {
            return List.of();
        }

        @Override
        public Optional<User> loadByAuthServiceUidOrUsername(final String authServiceUid, final String username) {
            return Optional.empty();
        }

        @Override
        public Optional<User> loadByAuthServiceUid(final String authServiceUid) {
            return Optional.empty();
        }

        @Override
        public int delete(final String username) {
            return 0;
        }

        @Override
        public int deleteById(final String userId) {
            return 0;
        }

        @Override
        public User create() {
            return null;
        }

        @Override
        public List<User> loadAll() {
            return List.of();
        }

        @Override
        public User getAdminUser() {
            return null;
        }

        @Override
        public Optional<User> getRootUser() {
            return Optional.empty();
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public List<User> loadAllForAuthServiceBackend(final String authServiceBackendId) {
            return List.of();
        }

        @Override
        public Collection<User> loadAllForRole(final Role role) {
            return List.of();
        }

        @Override
        public Set<String> getRoleNames(final User user) {
            return Set.of();
        }

        @Override
        public List<Permission> getPermissionsForUser(final User user) {
            return List.of();
        }

        @Override
        public List<WildcardPermission> getWildcardPermissionsForUser(final User user) {
            return List.of();
        }

        @Override
        public List<GRNPermission> getGRNPermissionsForUser(final User user) {
            return List.of();
        }

        @Override
        public Set<String> getUserPermissionsFromRoles(final User user) {
            return Set.of();
        }

        @Override
        public void dissociateAllUsersFromRole(final Role role) {

        }

        @Override
        public <T extends Persisted> int destroy(final T model) {
            return 0;
        }

        @Override
        public <T extends Persisted> int destroyAll(final Class<T> modelClass) {
            return 0;
        }

        @Override
        public <T extends Persisted> String save(final T model) throws ValidationException {
            return "";
        }

        @Nullable
        @Override
        public <T extends Persisted> String saveWithoutValidation(final T model) {
            return "";
        }

        @Override
        public <T extends Persisted> Map<String, List<ValidationResult>> validate(final T model, final Map<String, Object> fields) {
            return Map.of();
        }

        @Override
        public <T extends Persisted> Map<String, List<ValidationResult>> validate(final T model) {
            return Map.of();
        }

        @Override
        public Map<String, List<ValidationResult>> validate(final Map<String, Validator> validators, final Map<String, Object> fields) {
            return Map.of();
        }
    }
}
