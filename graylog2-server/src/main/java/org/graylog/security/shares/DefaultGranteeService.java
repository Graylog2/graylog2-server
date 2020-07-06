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
package org.graylog.security.shares;

import com.google.common.collect.ImmutableSet;
import org.graylog.security.shares.EntitySharePrepareResponse.AvailableGrantee;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.GRNRegistry;

import javax.inject.Inject;

public class DefaultGranteeService implements GranteeService {
    private final UserService userService;
    private final GRNRegistry grnRegistry;

    @Inject
    public DefaultGranteeService(UserService userService, GRNRegistry grnRegistry) {
        this.userService = userService;
        this.grnRegistry = grnRegistry;
    }

    @Override
    public ImmutableSet<AvailableGrantee> getAvailableGrantees(User sharingUser) {
        return ImmutableSet.<AvailableGrantee>builder()
                .addAll(getAvailableUserGrantees(sharingUser))
                .add(getGlobalGrantee())
                .build();
    }

    private ImmutableSet<AvailableGrantee> getAvailableUserGrantees(User sharingUser) {
        // TODO: We can only expose users that are in the same teams as the sharing user by default. There should
        //       also be a global config setting to allow exposing all existing users in the system.
        return userService.loadAll().stream()
                // Don't return the sharing user in available grantees until we want to support that sharing users
                // can remove themselves from an entity.
                .filter(user -> !sharingUser.getId().equals(user.getId()))
                .map(user -> AvailableGrantee.create(
                        grnRegistry.newGRN("user", user.getName()).toString(),
                        "user",
                        user.getFullName()
                ))
                .collect(ImmutableSet.toImmutableSet());
    }

    private AvailableGrantee getGlobalGrantee() {
        return AvailableGrantee.create(
                GRNRegistry.GLOBAL_USER_GRN,
                "global",
                "Everyone"
        );
    }

}
