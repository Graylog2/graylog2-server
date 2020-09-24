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

import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;

public class AuthServiceBackendUsageCheck {
    private final GlobalAuthServiceConfig globalAuthServiceConfig;
    private final UserService userService;

    @Inject
    public AuthServiceBackendUsageCheck(GlobalAuthServiceConfig globalAuthServiceConfig, UserService userService) {
        this.globalAuthServiceConfig = globalAuthServiceConfig;
        this.userService = userService;
    }

    public boolean isAuthServiceInUse(String authServiceBackendId) {
        // Check if the service is actively used
        final Optional<AuthServiceBackend> activeBackend = globalAuthServiceConfig.getActiveBackend();
        if (activeBackend.isPresent() && activeBackend.get().backendId().equals(authServiceBackendId)) {
            return true;
        }

        // Check if any users reference the service
        return userService.loadAllForAuthServiceBackend(authServiceBackendId).size() > 0;
    }
}
