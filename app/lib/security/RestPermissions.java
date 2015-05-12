/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package lib.security;

public enum RestPermissions {
    // this is the complete list from the graylog2-server corresponding to this interface version.
    // THESE MUST BE KEPT IN SYNC.
    // Yes, we should share the code in some other way, but currently we don't.

    USERS_CREATE("users:create"),
    USERS_EDIT("users:edit"),
    USERS_LIST("users:list"),
    USERS_PERMISSIONSEDIT("users:permissionsedit"),
    USERS_PASSWORDCHANGE("users:passwordchange"),
    USERS_TOKENCREATE("users:tokencreate"),
    USERS_TOKENLIST("users:tokenlist"),
    USERS_TOKENREMOVE("users:tokenremove"),
    THROUGHPUT_READ("throughput:read"),
    MESSAGECOUNT_READ("messagecount:read"),
    DASHBOARDS_CREATE("dashboards:create"),
    DASHBOARDS_LIST("dashboards:list"),
    DASHBOARDS_READ("dashboards:read"),
    DASHBOARDS_EDIT("dashboards:edit"),
    MESSAGES_READ("messages:read"),
    MESSAGES_ANALYZE("messages:analyze"),
    SEARCHES_ABSOLUTE("searches:absolute"),
    SEARCHES_KEYWORD("searches:keyword"),
    SEARCHES_RELATIVE("searches:relative"),
    SAVEDSEARCHES_CREATE("savedsearches:create"),
    SAVEDSEARCHES_READ("savedsearches:read"),
    SAVEDSEARCHES_EDIT("savedsearches:edit"),
    SOURCES_READ("sources:read"),
    STREAMS_CREATE("streams:create"),
    STREAMS_READ("streams:read"),
    STREAMS_EDIT("streams:edit"),
    STREAMS_CHANGESTATE("streams:changestate"),
    STREAM_OUTPUTS_CREATE("stream_outputs:create"),
    STREAM_OUTPUTS_READ("stream_outputs:read"),
    STREAM_OUTPUTS_DELETE("stream_outputs:delete"),
    INDEXERCLUSTER_READ("indexercluster:read"),
    INDICES_READ("indices:read"),
    INDICES_CHANGESTATE("indices:changestate"),
    INDICES_DELETE("indices:delete"),
    INDICES_FAILURES("indices:failures"),
    INPUTS_READ("inputs:read"),
    INPUTS_CREATE("inputs:create"),
    INPUTS_TERMINATE("inputs:terminate"),
    INPUTS_EDIT("inputs:edit"),
    INPUTS_STOP("inputs:stop"),
    INPUTS_START("inputs:start"),
    OUTPUTS_READ("outputs:read"),
    OUTPUTS_CREATE("outputs:create"),
    OUTPUTS_TERMINATE("outputs:terminate"),
    OUTPUTS_EDIT("outputs:edit"),
    SYSTEMJOBS_READ("systemjobs:read"),
    SYSTEMJOBS_CREATE("systemjobs:create"),
    LDAP_EDIT("ldap:edit"),
    LOGGERS_READ("loggers:read"),
    LOGGERS_EDIT("loggers:edit"),
    LOGGERS_READSUBSYSTEM("loggers:readsubsystem"),
    LOGGERS_EDITSUBSYSTEM("loggers:editsubsystem"),
    BUFFERS_READ("buffers:read"),
    DEFLECTOR_READ("deflector:read"),
    DEFLECTOR_CYCLE("deflector:cycle"),
    INDEXRANGES_READ("indexranges:read"),
    INDEXRANGES_REBUILD("indexranges:rebuild"),
    SYSTEMMESSAGES_READ("systemmessages:read"),
    METRICS_READALL("metrics:readall"),
    METRICS_ALLKEYS("metrics:allkeys"),
    METRICS_READ("metrics:read"),
    METRICS_READHISTORY("metrics:readhistory"),
    NOTIFICATIONS_READ("notifications:read"),
    NOTIFICATIONS_DELETE("notifications:delete"),
    SYSTEM_READ("system:read"),
    FIELDNAMES_READ("fieldnames:read"),
    PROCESSING_CHANGESTATE("processing:changestate"),
    JVMSTATS_READ("jvmstats:read"),
    THREADS_DUMP("threads:dump"),
    LBSTATUS_CHANGE("lbstatus:change"),
    NODE_SHUTDOWN("node:shutdown"),
    COLLECTORS_READ("collectors:read");

    private final String permission;

    RestPermissions(String objectActionWildcard) {
        this.permission = objectActionWildcard;
    }

    public String getPermission() {
        return permission;
    }
}
