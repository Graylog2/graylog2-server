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

import org.apache.shiro.authc.AuthenticationException;

/**
 * Thrown when authentication fails due to an external service being unavailable.
 */
public class AuthenticationServiceUnavailableException extends AuthenticationException {
    public AuthenticationServiceUnavailableException() {
        super();
    }

    public AuthenticationServiceUnavailableException(String message) {
        super(message);
    }

    public AuthenticationServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    public AuthenticationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
