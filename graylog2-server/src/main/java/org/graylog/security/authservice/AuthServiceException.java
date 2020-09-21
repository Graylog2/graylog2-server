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
package org.graylog.security.authservice;

import java.util.Locale;

public class AuthServiceException extends RuntimeException {
    private final String backendType;
    private final String backendId;

    private static String toMessage(String message, String backendType, String backendId) {
        return String.format(Locale.US, "AuthenticationService[%s/%s]: %s", backendType, backendId, message);
    }

    public AuthServiceException(String message, String backendType, String backendId) {
        super(toMessage(message, backendType, backendId));
        this.backendType = backendType;
        this.backendId = backendId;
    }

    public AuthServiceException(String message, String backendType, String backendId, Throwable cause) {
        super(toMessage(message, backendType, backendId), cause);
        this.backendType = backendType;
        this.backendId = backendId;
    }

    public String getBackendType() {
        return backendType;
    }

    public String getBackendId() {
        return backendId;
    }
}
