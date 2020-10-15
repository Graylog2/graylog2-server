/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security;

import com.google.inject.assistedinject.Assisted;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GranteeAuthorizer {
    public interface Factory {
        GranteeAuthorizer create(GRN grantee);
    }

    private final Subject subject;

    @Inject
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

    public boolean isPermitted(String permission, String id) {
        checkArgument(isNotBlank(permission), "permission cannot be null or empty");
        checkArgument(isNotBlank(id), "id cannot be null or empty");

        return subject.isPermitted(permission + ":" + id);
    }
}
