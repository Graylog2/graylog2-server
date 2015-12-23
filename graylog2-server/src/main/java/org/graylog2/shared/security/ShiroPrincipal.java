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

import org.apache.shiro.subject.Subject;

import java.security.Principal;

import static java.util.Objects.requireNonNull;

public class ShiroPrincipal implements Principal {
    private final Subject subject;

    public ShiroPrincipal(Subject subject) {
        this.subject = requireNonNull(subject);
    }

    @Override
    public String getName() {
        final Object principal = subject.getPrincipal();
        return principal == null ? null : principal.toString();
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public String toString() {

        return "ShiroPrincipal[" + getName() + "]";

    }
}
