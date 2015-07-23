package org.graylog2.security;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog2.shared.users.Role;
import org.graylog2.users.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryRolePermissionResolver implements RolePermissionResolver {
    private static final Logger log = LoggerFactory.getLogger(InMemoryRolePermissionResolver.class);

    private final RoleService roleService;
    private final AtomicReference<ImmutableMap<String, Role>> idToRoleIndex = new AtomicReference<>(ImmutableMap.<String, Role>of());

    @Inject
    public InMemoryRolePermissionResolver(RoleService roleService,
                                          @Named("daemonScheduler") ScheduledExecutorService daemonScheduler) {
        this.roleService = roleService;
        final RoleUpdater updater = new RoleUpdater();

        // eagerly load rules
        updater.run();

        // update rules every second in the background
        daemonScheduler.scheduleAtFixedRate(updater, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public Collection<Permission> resolvePermissionsInRole(String roleString) {
        final ImmutableMap<String, Role> index = idToRoleIndex.get();

        if (!index.containsKey(roleString)) {
            log.debug("Unknown role {}, cannot resolve permissions.", roleString);
            return null;
        }

        final Set<String> permissions = index.get(roleString).getPermissions();
        if (permissions == null) {
            log.debug("Role {} has no permissions assigned, cannot resolve permissions.", roleString);
            return null;
        }

        // copy to avoid reiterating all the time
        return Sets.newHashSet(Collections2.transform(permissions, new Function<String, Permission>() {
            @Nullable
            @Override
            public Permission apply(@Nullable String input) {
                return new WildcardPermission(input);
            }
        }));
    }

    private class RoleUpdater implements Runnable {
        @Override
        public void run() {
            try {
                final Set<Role> roles = roleService.loadAll();
                final ImmutableMap<String, Role> index = Maps.uniqueIndex(
                        roles,
                        new Function<Role, String>() {
                            @Nullable
                            @Override
                            public String apply(Role input) {
                                return input.getId();
                            }
                        });
                InMemoryRolePermissionResolver.this.idToRoleIndex.set(index);
            } catch (Exception e) {
                log.error("Could not find roles collection, no user roles updated.", e);
            }
        }
    }
}
