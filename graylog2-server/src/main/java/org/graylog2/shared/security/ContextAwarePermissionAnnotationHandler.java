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
package org.graylog2.shared.security;

import org.apache.shiro.authz.aop.PermissionAnnotationHandler;
import org.apache.shiro.subject.Subject;

import static java.util.Objects.requireNonNull;

public class ContextAwarePermissionAnnotationHandler extends PermissionAnnotationHandler {
    private final ShiroSecurityContext context;

    public ContextAwarePermissionAnnotationHandler(ShiroSecurityContext context) {
        this.context = requireNonNull(context);
    }

    @Override
    protected Subject getSubject() {
        return context.getSubject();
    }
}
