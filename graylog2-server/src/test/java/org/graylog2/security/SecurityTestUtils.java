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
import jakarta.ws.rs.core.SecurityContext;
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityTestUtils {

    // static holder for the current SecurityContext
    private static ShiroSecurityContext securityContext;

    public static ShiroSecurityContext getSecurityContext() {
        return securityContext;
    }

    public static void setSecurityContext(ShiroSecurityContext securityContext) {
        SecurityTestUtils.securityContext = securityContext;
    }

    /**
     * Creates a ShiroSecurityContext.
     * In the Context, a test realm is created with a role with the given name which has the given (wildcard) permissions.
     * The user with the given name is created and assigned the role.
     *
     * After this, the user is authenticated and set as the current principal in the ShiroSecurityContext.
     *
     * @param username    user name
     * @param rolename    role name
     * @param permissions wildcard permissions
     */
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

    /**
     * Removes all security related information from the thread context.
     */
    public static void clearSecurityContext() {
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
        SecurityUtils.setSecurityManager(null);
        SecurityTestUtils.setSecurityContext(null);
    }


    /**
     * Injects a security manager in RestResource instances.
     * This modifies the passed in instance to use the current SecurityContext from this holder.
     * Additionally, an interceptor is added to all instance methods that performs the required checks for @RequiresAuthentication
     * and @RequiresPermissions.
     *
     * @param resource instance object extending RestResource
     * @param clazz    class of the object
     * @return modified instance object with SecurityManager and method checks
     */
    public static <T extends RestResource> T injectSecurityManager(T resource, Class<T> clazz) {
        try {
            VarHandle handle = MethodHandles.privateLookupIn(RestResource.class, MethodHandles.lookup())
                    .findVarHandle(RestResource.class, "securityContext", SecurityContext.class);
            handle.set(resource, SecurityTestUtils.getSecurityContext());
            return Mockito.mock(clazz, Mockito.withSettings()
                    .spiedInstance(resource)
                    .defaultAnswer(new PermissionInterceptor())
            );

        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     */
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

    /**
     * Simple Shiro Realm to hold user/role information
     */
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
