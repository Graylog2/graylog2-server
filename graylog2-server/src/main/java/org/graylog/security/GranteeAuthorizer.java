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
package org.graylog.security;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog2.plugin.database.users.User;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GranteeAuthorizer {
    public interface Factory {
        GranteeAuthorizer create(GRN grantee);
        GranteeAuthorizer create(User grantee);
    }

    private final Subject subject;

    @AssistedInject
    public GranteeAuthorizer(DefaultSecurityManager securityManager,
                             GRNRegistry grnRegistry,
                             @Assisted User grantee) {
        this(securityManager, grnRegistry.ofUser(grantee));
    }

    @AssistedInject
    public GranteeAuthorizer(DefaultSecurityManager securityManager, @Assisted GRN grantee) {
        this.subject = new Subject.Builder(securityManager)
                .principals(new SimplePrincipalCollection(grantee, "GranteeAuthorizer"))
                .authenticated(true)
                .sessionCreationEnabled(false)
                .buildSubject();
    }

    public boolean isPermitted(String permission, GRN target) {
        return isPermitted(permission, target.entity());
    }

    public boolean isPermitted(String permission) {
        checkArgument(isNotBlank(permission), "permission cannot be null or empty");
        return subject.isPermitted(permission);
    }

    public boolean isPermitted(String permission, String id) {
        checkArgument(isNotBlank(permission), "permission cannot be null or empty");
        checkArgument(isNotBlank(id), "id cannot be null or empty");

        return subject.isPermitted(permission + ":" + id);
    }
}
