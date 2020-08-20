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
package org.graylog.security.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.Role;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class V20200811143600_ViewSharingToGrantsMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20200811143600_ViewSharingToGrantsMigration.class);

    // View sharing only allowed to give other users read permissions on searches and dashboards
    private static final Capability CAPABILITY = Capability.VIEW;

    private final DBGrantService grantService;
    private final MongoCollection<Document> collection;
    private final UserService userService;
    private final RoleService roleService;
    private final String rootUsername;
    private final ViewService viewService;

    @Inject
    public V20200811143600_ViewSharingToGrantsMigration(MongoConnection mongoConnection,
                                                        DBGrantService grantService,
                                                        UserService userService,
                                                        RoleService roleService,
                                                        @Named("root_username") String rootUsername,
                                                        ViewService viewService) {

        this.grantService = grantService;
        this.userService = userService;
        this.roleService = roleService;
        this.rootUsername = rootUsername;
        this.viewService = viewService;
        this.collection = mongoConnection.getMongoDatabase().getCollection("view_sharings", Document.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-08-11T14:36:00Z");
    }

    @Override
    public void upgrade() {
        for (final Document document : collection.find()) {
            LOG.debug("Migrate view sharing: {}", document);

            final ObjectId sharingId = document.getObjectId("_id");
            final String sharingType = document.get("type", String.class);
            final String viewId = document.get("view_id", String.class);

            try {
                switch (sharingType) {
                    case "users":
                        //noinspection unchecked
                        migrateUsers(viewId, (Collection<String>) document.get("users", Collection.class));
                        break;
                    case "roles":
                        //noinspection unchecked
                        migrateRoles(viewId, (Collection<String>) document.get("roles", Collection.class));
                        break;
                    case "all_of_instance":
                        migrateAllOfInstance(viewId);
                        break;
                    default:
                        LOG.warn("Skipping unknown view sharing type: {}", sharingType);
                        continue; // Continue here so we don't delete the sharing document
                }

                // The view sharing document should be removed after successful migration
                deleteViewSharing(sharingId);
            } catch (Exception e) {
                LOG.error("Couldn't migrate view sharing: {}", document, e);
            }
        }
    }

    private void migrateUsers(String viewId, Collection<String> users) {
        LOG.info("Migrate users for view {} to grants: {}", viewId, users);

        final GRN target = getTarget(viewId);

        for (final String user : users) {
            ensureGrant(user, target);
        }
    }

    private void migrateRoles(String viewId, Collection<String> roleNames) {
        LOG.info("Migrate roles for view {} to grants: {}", viewId, roleNames);

        final GRN target = getTarget(viewId);

        final Set<Role> roles = roleNames.stream()
                .map(roleName -> {
                    try {
                        return Optional.of(roleService.load(roleName));
                    } catch (NotFoundException e) {
                        return Optional.<Role>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        for (final Role role : roles) {
            for (final User user : userService.loadAllForRole(role)) {
                ensureGrant(user.getName(), target);
            }
        }
    }

    private void migrateAllOfInstance(String viewId) {
        LOG.info("Migrate all-of-instance for view {} to grants", viewId);

        final GRN target = getTarget(viewId);

        ensureEveryoneGrant(target);
    }

    private void ensureEveryoneGrant(GRN target) {
        final GRN grantee = GRNRegistry.GLOBAL_USER_GRN;

        if (!grantService.hasGrantFor(grantee, CAPABILITY, target)) {
            grantService.create(grantee, CAPABILITY, target, rootUsername);
        }
    }

    private void ensureGrant(String username, GRN target) {
        // TODO: Needs to use the user ID once we don't use user names in references anymore!
        final GRN grantee = GRNTypes.USER.toGRN(username);

        if (!grantService.hasGrantFor(grantee, CAPABILITY, target)) {
            grantService.create(grantee, CAPABILITY, target, rootUsername);
        }
    }

    private GRN getTarget(String viewId) {
        final ViewDTO view = viewService.get(viewId)
                .orElseThrow(() -> new IllegalArgumentException("View <" + viewId + "> doesn't exist"));
        final GRNType grnType = ViewDTO.Type.DASHBOARD.equals(view.type()) ? GRNTypes.DASHBOARD : GRNTypes.SEARCH;

        return grnType.toGRN(viewId);
    }

    private void deleteViewSharing(ObjectId id) {
        LOG.debug("Removing obsolete view sharing document {}", id);
        collection.deleteOne(Filters.eq("_id", id));
    }
}
