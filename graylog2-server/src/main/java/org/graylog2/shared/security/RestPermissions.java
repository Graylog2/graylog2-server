/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.security;

import com.google.common.collect.ImmutableSet;
import org.graylog.security.authzroles.BuiltinRole;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.security.Permission.create;

public class RestPermissions implements PluginPermissions {
    /**
     * These should all be in the form of "group:action", because {@link Permissions#allPermissionsMap()} below depends on it.
     * Should this ever change, you need to adapt the code below, too.
     */
    public static final String AUTH_HTTP_HEADER_CONFIG_EDIT = "authhttpheaderconfig:edit";
    public static final String AUTH_HTTP_HEADER_CONFIG_READ = "authhttpheaderconfig:read";
    public static final String AUTH_SERVICE_BACKEND_CREATE = "authservicebackend:create";
    public static final String AUTH_SERVICE_BACKEND_DELETE = "authservicebackend:delete";
    public static final String AUTH_SERVICE_BACKEND_EDIT = "authservicebackend:edit";
    public static final String AUTH_SERVICE_BACKEND_READ = "authservicebackend:read";
    public static final String AUTH_SERVICE_GLOBAL_CONFIG_READ = "authserviceglobalconfig:read";
    public static final String AUTH_SERVICE_GLOBAL_CONFIG_EDIT = "authserviceglobalconfig:edit";
    public static final String AUTH_SERVICE_TEST_BACKEND_EXECUTE = "authservicetestbackend:execute";
    public static final String BUFFERS_READ = "buffers:read";
    public static final String CATALOG_LIST = "catalog:list";
    public static final String CATALOG_RESOLVE = "catalog:resolve";
    public static final String CLUSTER_CONFIG_ENTRY_CREATE = "clusterconfigentry:create";
    public static final String CLUSTER_CONFIG_ENTRY_DELETE = "clusterconfigentry:delete";
    public static final String CLUSTER_CONFIG_ENTRY_EDIT = "clusterconfigentry:edit";
    public static final String CLUSTER_CONFIG_ENTRY_READ = "clusterconfigentry:read";
    public static final String CONTENT_PACK_CREATE = "contentpack:create";
    public static final String CONTENT_PACK_DELETE = "contentpack:delete";
    public static final String CONTENT_PACK_READ = "contentpack:read";
    public static final String CONTENT_PACK_INSTALL = "contentpack:install";
    public static final String CONTENT_PACK_UNINSTALL = "contentpack:uninstall";
    public static final String DASHBOARDS_CREATE = "dashboards:create";
    public static final String DASHBOARDS_EDIT = "dashboards:edit";
    public static final String DASHBOARDS_READ = "dashboards:read";
    public static final String DECORATORS_CREATE = "decorators:create";
    public static final String DECORATORS_EDIT = "decorators:edit";
    public static final String DECORATORS_READ = "decorators:read";
    public static final String DEFLECTOR_CYCLE = "deflector:cycle";
    public static final String DEFLECTOR_READ = "deflector:read";
    public static final String FREELICENSES_CREATE = "freelicenses:create";
    public static final String LICENSEINFOS_READ = "licenseinfos:read";
    public static final String EVENT_DEFINITIONS_CREATE = "eventdefinitions:create";
    public static final String EVENT_DEFINITIONS_DELETE = "eventdefinitions:delete";
    public static final String EVENT_DEFINITIONS_EDIT = "eventdefinitions:edit";
    public static final String EVENT_DEFINITIONS_EXECUTE = "eventdefinitions:execute";
    public static final String EVENT_DEFINITIONS_READ = "eventdefinitions:read";
    public static final String EVENT_NOTIFICATIONS_CREATE = "eventnotifications:create";
    public static final String EVENT_NOTIFICATIONS_DELETE = "eventnotifications:delete";
    public static final String EVENT_NOTIFICATIONS_EDIT = "eventnotifications:edit";
    public static final String EVENT_NOTIFICATIONS_READ = "eventnotifications:read";
    public static final String FIELDNAMES_READ = "fieldnames:read";
    public static final String GRANTS_OVERVIEW_READ = "grantsoverview:read";
    public static final String INDEXERCLUSTER_READ = "indexercluster:read";
    public static final String INDEXRANGES_READ = "indexranges:read";
    public static final String INDEXRANGES_REBUILD = "indexranges:rebuild";
    public static final String INDEXSETS_CREATE = "indexsets:create";
    public static final String INDEXSETS_DELETE = "indexsets:delete";
    public static final String INDEXSETS_EDIT = "indexsets:edit";
    public static final String INDEXSETS_READ = "indexsets:read";
    public static final String INDICES_CHANGESTATE = "indices:changestate";
    public static final String INDICES_DELETE = "indices:delete";
    public static final String INDICES_FAILURES = "indices:failures";
    public static final String INDICES_READ = "indices:read";
    public static final String INPUTS_CHANGESTATE = "inputs:changestate";
    public static final String INPUTS_CREATE = "inputs:create";
    public static final String INPUTS_EDIT = "inputs:edit";
    public static final String INPUTS_READ = "inputs:read";
    public static final String INPUTS_TERMINATE = "inputs:terminate";
    public static final String JOURNAL_EDIT = "journal:edit";
    public static final String JOURNAL_READ = "journal:read";
    public static final String JVMSTATS_READ = "jvmstats:read";
    public static final String LBSTATUS_CHANGE = "lbstatus:change";
    public static final String LOOKUP_TABLES_CREATE = "lookuptables:create";
    public static final String LOOKUP_TABLES_DELETE = "lookuptables:delete";
    public static final String LOOKUP_TABLES_EDIT = "lookuptables:edit";
    public static final String LOOKUP_TABLES_READ = "lookuptables:read";
    public static final String LOGGERS_EDIT = "loggers:edit";
    public static final String LOGGERS_EDITSUBSYSTEM = "loggers:editsubsystem";
    public static final String LOGGERS_READ = "loggers:read";
    public static final String LOGGERS_READSUBSYSTEM = "loggers:readsubsystem";
    public static final String LOGGERSMESSAGES_READ = "loggersmessages:read";
    public static final String MESSAGECOUNT_READ = "messagecount:read";
    public static final String MESSAGES_ANALYZE = "messages:analyze";
    public static final String MESSAGES_READ = "messages:read";
    public static final String METRICS_ALLKEYS = "metrics:allkeys";
    public static final String METRICS_READ = "metrics:read";
    public static final String METRICS_READALL = "metrics:readall";
    public static final String METRICS_READHISTORY = "metrics:readhistory";
    public static final String NODE_SHUTDOWN = "node:shutdown";
    public static final String NOTIFICATIONS_DELETE = "notifications:delete";
    public static final String NOTIFICATIONS_READ = "notifications:read";
    public static final String OUTPUTS_CREATE = "outputs:create";
    public static final String OUTPUTS_EDIT = "outputs:edit";
    public static final String OUTPUTS_READ = "outputs:read";
    public static final String OUTPUTS_TERMINATE = "outputs:terminate";
    public static final String PROCESSING_CHANGESTATE = "processing:changestate";
    public static final String ROLES_CREATE = "roles:create";
    public static final String ROLES_DELETE = "roles:delete";
    public static final String ROLES_EDIT = "roles:edit";
    public static final String ROLES_READ = "roles:read";
    public static final String SEARCHES_ABSOLUTE = "searches:absolute";
    public static final String SEARCHES_KEYWORD = "searches:keyword";
    public static final String SEARCHES_RELATIVE = "searches:relative";
    public static final String SOURCES_READ = "sources:read";
    public static final String STREAM_OUTPUTS_CREATE = "stream_outputs:create";
    public static final String STREAM_OUTPUTS_DELETE = "stream_outputs:delete";
    public static final String STREAM_OUTPUTS_READ = "stream_outputs:read";
    public static final String STREAMS_CHANGESTATE = "streams:changestate";
    public static final String STREAMS_CREATE = "streams:create";
    public static final String STREAMS_EDIT = "streams:edit";
    public static final String STREAMS_READ = "streams:read";
    public static final String SYSTEM_READ = "system:read";
    public static final String SYSTEMJOBS_CREATE = "systemjobs:create";
    public static final String SYSTEMJOBS_DELETE = "systemjobs:delete";
    public static final String SYSTEMJOBS_READ = "systemjobs:read";
    public static final String SYSTEMMESSAGES_READ = "systemmessages:read";
    public static final String THREADS_DUMP = "threads:dump";
    public static final String PROCESSBUFFER_DUMP = "processbuffer:dump";
    public static final String THROUGHPUT_READ = "throughput:read";
    public static final String URL_WHITELIST_READ = "urlwhitelist:read";
    public static final String URL_WHITELIST_WRITE = "urlwhitelist:write";
    public static final String USERS_CREATE = "users:create";
    public static final String USERS_EDIT = "users:edit";
    public static final String USERS_READ = "users:read";
    public static final String USERS_LIST = "users:list";
    public static final String USERS_PASSWORDCHANGE = "users:passwordchange";
    public static final String USERS_PERMISSIONSEDIT = "users:permissionsedit";
    public static final String USERS_ROLESEDIT = "users:rolesedit";
    public static final String USERS_TOKENCREATE = "users:tokencreate";
    public static final String USERS_TOKENLIST = "users:tokenlist";
    public static final String USERS_TOKENREMOVE = "users:tokenremove";

    // This is a special permission that ONLY works with GRNs as ID/target
    // TODO does this belong here?
    public static final String ENTITY_OWN = "entity:own";

    protected static final ImmutableSet<Permission> PERMISSIONS = ImmutableSet.<Permission>builder()
            .add(create(AUTH_HTTP_HEADER_CONFIG_EDIT, ""))
            .add(create(AUTH_HTTP_HEADER_CONFIG_READ, ""))
            .add(create(AUTH_SERVICE_BACKEND_CREATE, ""))
            .add(create(AUTH_SERVICE_BACKEND_DELETE, ""))
            .add(create(AUTH_SERVICE_BACKEND_EDIT, ""))
            .add(create(AUTH_SERVICE_BACKEND_READ, ""))
            .add(create(AUTH_SERVICE_GLOBAL_CONFIG_READ, ""))
            .add(create(AUTH_SERVICE_GLOBAL_CONFIG_EDIT, ""))
            .add(create(AUTH_SERVICE_TEST_BACKEND_EXECUTE, ""))
            .add(create(BUFFERS_READ, ""))
            .add(create(CONTENT_PACK_CREATE, ""))
            .add(create(CONTENT_PACK_DELETE, ""))
            .add(create(CONTENT_PACK_READ, ""))
            .add(create(CATALOG_LIST, ""))
            .add(create(CATALOG_RESOLVE, ""))
            .add(create(CLUSTER_CONFIG_ENTRY_CREATE, ""))
            .add(create(CLUSTER_CONFIG_ENTRY_DELETE, ""))
            .add(create(CLUSTER_CONFIG_ENTRY_EDIT, ""))
            .add(create(CLUSTER_CONFIG_ENTRY_READ, ""))
            .add(create(DASHBOARDS_CREATE, ""))
            .add(create(DASHBOARDS_EDIT, ""))
            .add(create(DASHBOARDS_READ, ""))
            .add(create(DECORATORS_CREATE, ""))
            .add(create(DECORATORS_EDIT, ""))
            .add(create(DECORATORS_READ, ""))
            .add(create(DEFLECTOR_CYCLE, ""))
            .add(create(DEFLECTOR_READ, ""))
            .add(create(FREELICENSES_CREATE, ""))
            .add(create(LICENSEINFOS_READ, ""))
            .add(create(EVENT_DEFINITIONS_CREATE, ""))
            .add(create(EVENT_DEFINITIONS_DELETE, ""))
            .add(create(EVENT_DEFINITIONS_EDIT, ""))
            .add(create(EVENT_DEFINITIONS_EXECUTE, ""))
            .add(create(EVENT_DEFINITIONS_READ, ""))
            .add(create(EVENT_NOTIFICATIONS_CREATE, ""))
            .add(create(EVENT_NOTIFICATIONS_DELETE, ""))
            .add(create(EVENT_NOTIFICATIONS_EDIT, ""))
            .add(create(EVENT_NOTIFICATIONS_READ, ""))
            .add(create(FIELDNAMES_READ, ""))
            .add(create(GRANTS_OVERVIEW_READ, ""))
            .add(create(INDEXERCLUSTER_READ, ""))
            .add(create(INDEXRANGES_READ, ""))
            .add(create(INDEXRANGES_REBUILD, ""))
            .add(create(INDEXSETS_CREATE, ""))
            .add(create(INDEXSETS_DELETE, ""))
            .add(create(INDEXSETS_EDIT, ""))
            .add(create(INDEXSETS_READ, ""))
            .add(create(INDICES_CHANGESTATE, ""))
            .add(create(INDICES_DELETE, ""))
            .add(create(INDICES_FAILURES, ""))
            .add(create(INDICES_READ, ""))
            .add(create(INPUTS_CHANGESTATE, ""))
            .add(create(INPUTS_CREATE, ""))
            .add(create(INPUTS_EDIT, ""))
            .add(create(INPUTS_READ, ""))
            .add(create(INPUTS_TERMINATE, ""))
            .add(create(JOURNAL_EDIT, ""))
            .add(create(JOURNAL_READ, ""))
            .add(create(JVMSTATS_READ, ""))
            .add(create(LBSTATUS_CHANGE, ""))
            .add(create(LOOKUP_TABLES_CREATE, ""))
            .add(create(LOOKUP_TABLES_DELETE, ""))
            .add(create(LOOKUP_TABLES_EDIT, ""))
            .add(create(LOOKUP_TABLES_READ, ""))
            .add(create(LOGGERS_EDIT, ""))
            .add(create(LOGGERS_EDITSUBSYSTEM, ""))
            .add(create(LOGGERS_READ, ""))
            .add(create(LOGGERS_READSUBSYSTEM, ""))
            .add(create(LOGGERSMESSAGES_READ, ""))
            .add(create(MESSAGECOUNT_READ, ""))
            .add(create(MESSAGES_ANALYZE, ""))
            .add(create(MESSAGES_READ, ""))
            .add(create(METRICS_ALLKEYS, ""))
            .add(create(METRICS_READ, ""))
            .add(create(METRICS_READALL, ""))
            .add(create(METRICS_READHISTORY, ""))
            .add(create(NODE_SHUTDOWN, ""))
            .add(create(NOTIFICATIONS_DELETE, ""))
            .add(create(NOTIFICATIONS_READ, ""))
            .add(create(OUTPUTS_CREATE, ""))
            .add(create(OUTPUTS_EDIT, ""))
            .add(create(OUTPUTS_READ, ""))
            .add(create(OUTPUTS_TERMINATE, ""))
            .add(create(PROCESSING_CHANGESTATE, ""))
            .add(create(ROLES_CREATE, ""))
            .add(create(ROLES_DELETE, ""))
            .add(create(ROLES_EDIT, ""))
            .add(create(ROLES_READ, ""))
            .add(create(SEARCHES_ABSOLUTE, ""))
            .add(create(SEARCHES_KEYWORD, ""))
            .add(create(SEARCHES_RELATIVE, ""))
            .add(create(SOURCES_READ, ""))
            .add(create(STREAM_OUTPUTS_CREATE, ""))
            .add(create(STREAM_OUTPUTS_DELETE, ""))
            .add(create(STREAM_OUTPUTS_READ, ""))
            .add(create(STREAMS_CHANGESTATE, ""))
            .add(create(STREAMS_CREATE, ""))
            .add(create(STREAMS_EDIT, ""))
            .add(create(STREAMS_READ, ""))
            .add(create(SYSTEM_READ, ""))
            .add(create(SYSTEMJOBS_CREATE, ""))
            .add(create(SYSTEMJOBS_DELETE, ""))
            .add(create(SYSTEMJOBS_READ, ""))
            .add(create(SYSTEMMESSAGES_READ, ""))
            .add(create(THREADS_DUMP, ""))
            .add(create(PROCESSBUFFER_DUMP, ""))
            .add(create(THROUGHPUT_READ, ""))
            .add(create(URL_WHITELIST_READ, ""))
            .add(create(URL_WHITELIST_WRITE, ""))
            .add(create(USERS_CREATE, ""))
            .add(create(USERS_EDIT, ""))
            .add(create(USERS_READ, ""))
            .add(create(USERS_LIST, ""))
            .add(create(USERS_PASSWORDCHANGE, ""))
            .add(create(USERS_PERMISSIONSEDIT, ""))
            .add(create(USERS_ROLESEDIT, ""))
            .add(create(USERS_TOKENCREATE, ""))
            .add(create(USERS_TOKENLIST, ""))
            .add(create(USERS_TOKENREMOVE, ""))
            .build();

    // Standard set of PERMISSIONS of readers.
    protected static final ImmutableSet<String> READER_BASE_PERMISSION_SELECTION = ImmutableSet.<String>builder().add(
            BUFFERS_READ,
            CLUSTER_CONFIG_ENTRY_READ,
            DECORATORS_READ,
            FIELDNAMES_READ,
            INDEXERCLUSTER_READ,
            INPUTS_READ,
            JOURNAL_READ,
            JVMSTATS_READ,
            MESSAGECOUNT_READ,
            MESSAGES_ANALYZE,
            MESSAGES_READ,
            METRICS_READ,
            SYSTEM_READ,
            THROUGHPUT_READ
    ).build();

    protected static final Set<Permission> READER_BASE_PERMISSIONS = PERMISSIONS.stream()
            .filter(permission -> READER_BASE_PERMISSION_SELECTION.contains(permission.permission()))
            .collect(Collectors.toSet());

    protected static final ImmutableSet<BuiltinRole> BUILTIN_ROLES = ImmutableSet.<BuiltinRole>builder().add(
            BuiltinRole.create("Dashboard Creator", "Allows creation of Dashboards (built-in)", ImmutableSet.of(
                    RestPermissions.DASHBOARDS_CREATE
            )),
            BuiltinRole.create("Event Definition Creator", "Allows creation of Event Definitions (built-in)", ImmutableSet.of(
                    RestPermissions.EVENT_DEFINITIONS_CREATE
            )),
            BuiltinRole.create("Event Notification Creator", "Allows creation of Event Notifications (built-in)", ImmutableSet.of(
                    RestPermissions.EVENT_NOTIFICATIONS_CREATE
            )),
            BuiltinRole.create("User Inspector", "Allows listing all user accounts (built-in)", ImmutableSet.of(
                    RestPermissions.USERS_READ
            ))
    ).build();

    @Override
    public Set<Permission> readerBasePermissions() {
        return READER_BASE_PERMISSIONS;
    }

    @Override
    public Set<Permission> permissions() {
        return PERMISSIONS;
    }

    @Override
    public Set<BuiltinRole> builtinRoles() {
        return BUILTIN_ROLES;
    }
}
