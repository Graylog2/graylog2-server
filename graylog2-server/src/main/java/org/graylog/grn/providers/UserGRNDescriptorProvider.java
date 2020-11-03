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
package org.graylog.grn.providers;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorProvider;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Optional;

public class UserGRNDescriptorProvider implements GRNDescriptorProvider {
    private final UserService userService;

    @Inject
    public UserGRNDescriptorProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public GRNDescriptor get(GRN grn) {
        return Optional.ofNullable(userService.loadById(grn.entity()))
                .map(user -> GRNDescriptor.create(grn, user.getFullName()))
                .orElse(GRNDescriptor.create(grn, "ERROR: User for <" + grn.toString() + "> not found!"));
    }
}
