/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class RestPermissions {
    private static final Logger LOG = LoggerFactory.getLogger(RestPermissions.class);

    // These should all be in the form of "group:action", because allPermissions() below depends on it.
    // Should this ever change, you need to adapt the code below, too.
    public static final String USERS_CREATE = "users:create";
    public static final String USERS_EDIT = "users:edit";
    public static final String USERS_LIST = "users:list";
    public static final String USERS_PERMISSIONSEDIT = "users:permissionsedit";
    public static final String USERS_PASSWORDCHANGE = "users:passwordchange";
    public static final String USERS_TOKENCREATE = "users:tokencreate";
    public static final String USERS_TOKENLIST = "users:tokenlist";
    public static final String USERS_TOKENREMOVE = "users:tokenremove";
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

    private static Map<String, Collection<String>> allPermissions;

    public static Set<String> adminPermissions = Sets.newHashSet("*");

    // Standard set of permissions of readers.
    public static Set<String> readerBasePermissions = Sets.newHashSet(
            BUFFERS_READ,
            FIELDNAMES_READ,
            INDEXERCLUSTER_READ,
            INPUTS_READ,
            JVMSTATS_READ,
            MESSAGECOUNT_READ,
            MESSAGES_READ,
            METRICS_READ,
            SYSTEM_READ,
            THROUGHPUT_READ,
            SAVEDSEARCHES_CREATE,
            SAVEDSEARCHES_EDIT,
            SAVEDSEARCHES_READ
    );

    public static Set<String> readerPermissions(String username) {
        final HashSet<String> perms = Sets.newHashSet(readerBasePermissions);
        if (username == null || username.isEmpty()) {
            LOG.error("Username cannot be empty or null for creating reader permissions");
            throw new IllegalArgumentException("Username was null or empty when getting reader permissions.");
        }
        perms.add(perInstance(USERS_EDIT, username));
        perms.add(perInstance(USERS_PASSWORDCHANGE, username));
        return perms;
    }

    public static String perInstance(String permission, String instance) {
        // TODO check for existing instance etc (use DomainPermission subclass)
        return permission + ":" + instance;
    }

    public static synchronized Map<String, Collection<String>> allPermissions() {
        if (allPermissions == null) {
            final Field[] declaredFields = RestPermissions.class.getDeclaredFields();
            ListMultimap<String, String> all = ArrayListMultimap.create();
            for (Field declaredField : declaredFields) {
                if (! Modifier.isStatic(declaredField.getModifiers())) {
                    continue;
                }
                if (! String.class.isAssignableFrom(declaredField.getType())) {
                    continue;
                }
                declaredField.setAccessible(true);
                try {
                    final String permission = (String) declaredField.get(RestPermissions.class);
                    final Iterator<String> split = Splitter.on(':').limit(2).split(permission).iterator();
                    final String group = split.next();
                    final String action = split.next();
                    all.put(group, action);
                } catch (IllegalAccessException ignored) {
                }
            }
            allPermissions = all.asMap();
        }
        return allPermissions;
    }
}
