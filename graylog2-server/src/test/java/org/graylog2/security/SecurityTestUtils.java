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

package org.graylog2.security;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityTestUtils {

    private static ShiroSecurityContext securityContext;

    public static ShiroSecurityContext getSecurityContext() {
        return securityContext;
    }

    public static void setSecurityContext(ShiroSecurityContext securityContext) {
        SecurityTestUtils.securityContext = securityContext;
    }

    public static void setupSecurityContext(String username, String rolename, Set<String> permissions) {
        String password = "test_password";

        TestRealm realm = new TestRealm();
        realm.addRole(rolename, permissions);
        realm.addUser(username, password, rolename);

        SecurityManager securityManager = new DefaultSecurityManager(realm);
        SecurityUtils.setSecurityManager(securityManager);

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        subject.login(token);

        ThreadContext.bind(subject);

        SecurityTestUtils.setSecurityContext(new ShiroSecurityContext(subject, token, true, null, new MultivaluedHashMap<>()));
    }

    public static void clearSecurityContext() {
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
        SecurityUtils.setSecurityManager(null);
        SecurityTestUtils.setSecurityContext(null);
    }


    public static <T extends RestResource> T injectSecurityManager(T resource, Class<T> clazz) {
        try {
            Field securityContextField = RestResource.class.getDeclaredField("securityContext");
            securityContextField.setAccessible(true);
            securityContextField.set(resource, SecurityTestUtils.getSecurityContext());

            return Mockito.mock(clazz, Mockito.withSettings()
                    .spiedInstance(resource)
                    .defaultAnswer(new PermissionInterceptor())
            );

        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static class PermissionInterceptor implements Answer<Object> {

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            if ((
                    invocationOnMock.getMock().getClass().isAnnotationPresent(RequiresAuthentication.class) ||
                            invocationOnMock.getMethod().isAnnotationPresent(RequiresAuthentication.class)
            ) && (
                    SecurityTestUtils.getSecurityContext() == null ||
                            !SecurityTestUtils.getSecurityContext().getSubject().isAuthenticated()
            )) {
                throw new ForbiddenException("Not authenticated");
            }
            if (invocationOnMock.getMock().getClass().isAnnotationPresent(RequiresPermissions.class)) {
                checkPermissions(invocationOnMock.getMock().getClass().getAnnotation(RequiresPermissions.class));
            }
            if (invocationOnMock.getMethod().isAnnotationPresent(RequiresPermissions.class)) {
                checkPermissions(invocationOnMock.getMethod().getAnnotation(RequiresPermissions.class));
            }
            return invocationOnMock.callRealMethod();
        }

        private void checkPermissions(RequiresPermissions annotation) {
            String[] requiredPermissions = annotation.value();
            boolean[] permitted = SecurityTestUtils.getSecurityContext().getSubject().isPermitted(requiredPermissions);
            if (annotation.logical().equals(Logical.AND) && ArrayUtils.contains(permitted, false)) {
                throw new ForbiddenException("Not authorized. Necessary permissions are: " + String.join(",", requiredPermissions));
            }
            ;
            if (annotation.logical().equals(Logical.OR) && !ArrayUtils.contains(permitted, true)) {
                throw new ForbiddenException("Not authorized. Necessary permissions are: " + String.join(",", requiredPermissions));
            }
        }
    }

    static class TestRealm extends SimpleAccountRealm {


        public void addRole(String roleName, Set<String> permissions) {
            SimpleRole role = new SimpleRole(roleName, permissions.stream().map(WildcardPermission::new).collect(Collectors.toSet()));
            super.add(role);
        }

        public void addUser(String userName, String password, String roleName) {
            SimpleAccount user = new SimpleAccount(userName, password, getName(), Set.of(roleName), getRole(roleName).getPermissions());
            super.add(user);
        }

    }

}
