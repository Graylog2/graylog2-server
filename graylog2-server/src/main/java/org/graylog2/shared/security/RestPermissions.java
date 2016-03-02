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

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import org.graylog2.plugin.security.RestPermission;
import org.graylog2.plugin.security.RestPermissionsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class RestPermissions {
    private static final Logger LOG = LoggerFactory.getLogger(RestPermissions.class);

    /**
     *These should all be in the form of "group:action", because {@link #allPermissions()} below depends on it.
     * Should this ever change, you need to adapt the code below, too.
     */
    public static final String USERS_CREATE = "users:create";
    public static final String USERS_EDIT = "users:edit";
    public static final String USERS_LIST = "users:list";
    public static final String USERS_PERMISSIONSEDIT = "users:permissionsedit";
    public static final String USERS_PASSWORDCHANGE = "users:passwordchange";
    public static final String USERS_TOKENCREATE = "users:tokencreate";
    public static final String USERS_TOKENLIST = "users:tokenlist";
    public static final String USERS_TOKENREMOVE = "users:tokenremove";
    public static final String USERS_ROLESEDIT = "users:rolesedit";
    public static final String THROUGHPUT_READ = "throughput:read";
    public static final String MESSAGECOUNT_READ = "messagecount:read";
    public static final String DASHBOARDS_CREATE = "dashboards:create";
    public static final String DASHBOARDS_READ = "dashboards:read";
    public static final String DASHBOARDS_EDIT = "dashboards:edit";
    public static final String MESSAGES_READ = "messages:read";
    public static final String MESSAGES_ANALYZE = "messages:analyze";
    public static final String SEARCHES_ABSOLUTE = "searches:absolute";
    public static final String SEARCHES_KEYWORD = "searches:keyword";
    public static final String SEARCHES_RELATIVE = "searches:relative";
    public static final String SAVEDSEARCHES_CREATE = "savedsearches:create";
    public static final String SAVEDSEARCHES_READ = "savedsearches:read";
    public static final String SAVEDSEARCHES_EDIT = "savedsearches:edit";
    public static final String SOURCES_READ = "sources:read";
    public static final String STREAMS_CREATE = "streams:create";
    public static final String STREAMS_READ = "streams:read";
    public static final String STREAMS_EDIT = "streams:edit";
    public static final String STREAMS_CHANGESTATE = "streams:changestate";
    public static final String STREAM_OUTPUTS_CREATE = "stream_outputs:create";
    public static final String STREAM_OUTPUTS_READ = "stream_outputs:read";
    public static final String STREAM_OUTPUTS_DELETE = "stream_outputs:delete";
    public static final String INDEXERCLUSTER_READ = "indexercluster:read";
    public static final String INDICES_READ = "indices:read";
    public static final String INDICES_CHANGESTATE = "indices:changestate";
    public static final String INDICES_DELETE = "indices:delete";
    public static final String INDICES_FAILURES = "indices:failures";
    public static final String INPUTS_READ = "inputs:read";
    public static final String INPUTS_CREATE = "inputs:create";
    public static final String INPUTS_TERMINATE = "inputs:terminate";
    public static final String INPUTS_EDIT = "inputs:edit";
    public static final String OUTPUTS_READ = "outputs:read";
    public static final String OUTPUTS_CREATE = "outputs:create";
    public static final String OUTPUTS_TERMINATE = "outputs:terminate";
    public static final String OUTPUTS_EDIT = "outputs:edit";
    public static final String SYSTEMJOBS_READ = "systemjobs:read";
    public static final String SYSTEMJOBS_CREATE = "systemjobs:create";
    public static final String LDAP_EDIT = "ldap:edit";
    public static final String LDAPGROUPS_READ = "ldapgroups:read";
    public static final String LDAPGROUPS_EDIT = "ldapgroups:edit";
    public static final String LOGGERS_READ = "loggers:read";
    public static final String LOGGERS_EDIT = "loggers:edit";
    public static final String LOGGERS_READSUBSYSTEM = "loggers:readsubsystem";
    public static final String LOGGERS_EDITSUBSYSTEM = "loggers:editsubsystem";
    public static final String BUFFERS_READ = "buffers:read";
    public static final String DEFLECTOR_READ = "deflector:read";
    public static final String DEFLECTOR_CYCLE = "deflector:cycle";
    public static final String INDEXRANGES_READ = "indexranges:read";
    public static final String INDEXRANGES_REBUILD = "indexranges:rebuild";
    public static final String SYSTEMMESSAGES_READ = "systemmessages:read";
    public static final String METRICS_READALL = "metrics:readall";
    public static final String METRICS_ALLKEYS = "metrics:allkeys";
    public static final String METRICS_READ = "metrics:read";
    public static final String METRICS_READHISTORY = "metrics:readhistory";
    public static final String NOTIFICATIONS_READ = "notifications:read";
    public static final String NOTIFICATIONS_DELETE = "notifications:delete";
    public static final String SYSTEM_READ = "system:read";
    public static final String FIELDNAMES_READ = "fieldnames:read";
    public static final String PROCESSING_CHANGESTATE = "processing:changestate";
    public static final String JVMSTATS_READ = "jvmstats:read";
    public static final String THREADS_DUMP = "threads:dump";
    public static final String NODE_SHUTDOWN = "node:shutdown";
    public static final String LBSTATUS_CHANGE = "lbstatus:change";
    public static final String BLACKLISTENTRY_CREATE = "blacklistentry:create";
    public static final String BLACKLISTENTRY_READ = "blacklistentry:read";
    public static final String BLACKLISTENTRY_EDIT = "blacklistentry:edit";
    public static final String BLACKLISTENTRY_DELETE = "blacklistentry:delete";
    public static final String BUNDLE_CREATE = "bundle:create";
    public static final String BUNDLE_READ = "bundle:read";
    public static final String BUNDLE_UPDATE = "bundle:update";
    public static final String BUNDLE_DELETE = "bundle:delete";
    public static final String BUNDLE_IMPORT = "bundle:import";
    public static final String BUNDLE_EXPORT = "bundle:export";
    public static final String JOURNAL_READ = "journal:read";
    public static final String JOURNAL_EDIT = "journal:edit";
    public static final String COLLECTORS_READ = "collectors:read";
    public static final String ROLES_CREATE = "roles:create";
    public static final String ROLES_READ = "roles:read";
    public static final String ROLES_EDIT = "roles:edit";
    public static final String ROLES_DELETE = "roles:delete";
    public static final String CLUSTER_CONFIG_ENTRY_CREATE = "clusterconfigentry:create";
    public static final String CLUSTER_CONFIG_ENTRY_READ = "clusterconfigentry:read";
    public static final String CLUSTER_CONFIG_ENTRY_EDIT = "clusterconfigentry:edit";
    public static final String CLUSTER_CONFIG_ENTRY_DELETE = "clusterconfigentry:delete";

    // Standard set of permissions of readers.
    private final Set<String> readerBasePermissions = ImmutableSet.<String>builder().add(
                    BUFFERS_READ,
                    FIELDNAMES_READ,
                    INDEXERCLUSTER_READ,
                    INPUTS_READ,
                    JOURNAL_READ,
                    JVMSTATS_READ,
                    MESSAGECOUNT_READ,
                    MESSAGES_READ,
                    METRICS_READ,
                    SYSTEM_READ,
                    THROUGHPUT_READ,
                    SAVEDSEARCHES_CREATE,
                    SAVEDSEARCHES_EDIT,
                    SAVEDSEARCHES_READ
    ).build();

    private final Map<String, Collection<String>> allPermissions;

    public RestPermissions() {
        this(new HashSet<>());
    }

    @Inject
    public RestPermissions(final Set<RestPermissionsPlugin> pluginPermissions) {
        this.allPermissions = buildAllPermissions(pluginPermissions);
    }

    public Set<String> readerBasePermissions() {
        return readerBasePermissions;
    }

    public Set<String> readerPermissions(String username) {
        final ImmutableSet.Builder<String> perms = ImmutableSet.<String>builder().addAll(readerBasePermissions);
        if (isNullOrEmpty(username)) {
            LOG.error("Username cannot be empty or null for creating reader permissions");
            throw new IllegalArgumentException("Username was null or empty when getting reader permissions.");
        }

        perms.addAll(userSelfEditPermissions(username));

        return perms.build();
    }

    public Set<String> userSelfEditPermissions(String username) {
        ImmutableSet.Builder<String> perms = ImmutableSet.builder();
        perms.add(perInstance(USERS_EDIT, username));
        perms.add(perInstance(USERS_PASSWORDCHANGE, username));
        return perms.build();
    }

    private String perInstance(String permission, String instance) {
        // TODO check for existing instance etc (use DomainPermission subclass)
        return permission + ":" + instance;
    }

    public Map<String, Collection<String>> allPermissions() {
        return allPermissions;
    }

    private static Map<String, Collection<String>> buildAllPermissions(Set<RestPermissionsPlugin> pluginPermissions) {
        final ListMultimap<String, String> all = ArrayListMultimap.create();

        preparePermissions(all, getPermissionsForClass(RestPermissions.class));

        for (RestPermissionsPlugin pluginPermission : pluginPermissions) {
            final Set<String> permissions = pluginPermission.permissions().stream()
                    .map(RestPermission::value)
                    .collect(Collectors.toSet());

            try {
                preparePermissions(all, permissions);
            } catch (IllegalArgumentException e) {
                LOG.error("Error adding permissions for plugin: " + pluginPermission.getClass().getCanonicalName(), e);
                throw e;
            }
        }

        return all.asMap();
    }

    private static void preparePermissions(final ListMultimap<String, String> all, final Set<String> permissions) {
        for (String permission : permissions) {
            final Iterator<String> split = Splitter.on(':').limit(2).split(permission).iterator();
            final String group = split.next();
            final String action = split.next();

            if (all.containsKey(group) && all.get(group).contains(action)) {
                throw new IllegalArgumentException("Duplicate permission found. Permission \"" + permission + "\" already exists!");
            }

            all.put(group, action);
        }
    }

    private static Set<String> getPermissionsForClass(Class<?> permissionsClass) {
        final Field[] declaredFields = permissionsClass.getDeclaredFields();
        final Set<String> permissions = new HashSet<>();

        for (Field declaredField : declaredFields) {
            if (!Modifier.isStatic(declaredField.getModifiers())) {
                continue;
            }
            if (!String.class.isAssignableFrom(declaredField.getType())) {
                continue;
            }
            declaredField.setAccessible(true);
            try {
                final String permission = (String) declaredField.get(permissionsClass);
                permissions.add(permission);
            } catch (IllegalAccessException ignored) {
            }
        }

        return permissions;
    }
}
