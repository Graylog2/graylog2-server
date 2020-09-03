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
package org.graylog2.migrations.V20200803120800_GrantsMigrations;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Optional;

public class ViewOwnerShipToGrantsMigration {
    private static final Logger LOG = LoggerFactory.getLogger(ViewOwnerShipToGrantsMigration.class);
    private final UserService userService;
    private final DBGrantService dbGrantService;
    private final String rootUsername;
    private final ViewService viewService;

    private static final Capability CAPABILITY = Capability.OWN;

    public ViewOwnerShipToGrantsMigration(UserService userService,
                                          DBGrantService dbGrantService,
                                          @Named("root_username") String rootUsername,
                                          ViewService viewService) {
        this.userService = userService;
        this.dbGrantService = dbGrantService;
        this.rootUsername = rootUsername;
        this.viewService = viewService;
    }

    public void upgrade() {
        viewService.streamAll().forEach(view -> {
            final Optional<User> user = view.owner().map(userService::load);
            if (user.isPresent() && !user.get().isLocalAdmin()) {
                final GRNType grnType = ViewDTO.Type.DASHBOARD.equals(view.type()) ? GRNTypes.DASHBOARD : GRNTypes.SEARCH;
                final GRN target = grnType.toGRN(view.id());

                ensureGrant(user.get().getName(), target);
            }
        });
    }

    private void ensureGrant(String username, GRN target) {
        // TODO: Needs to use the user ID once we don't use user names in references anymore!
        final GRN grantee = GRNTypes.USER.toGRN(username);

        if (!dbGrantService.hasGrantFor(grantee, CAPABILITY, target)) {
            LOG.info("Registering user <{}> ownership for <{}>", username, target);
            dbGrantService.create(grantee, CAPABILITY, target, rootUsername);
        }
    }
}
