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
package org.graylog.scheduler.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class JobSchedulerAuditEventTypes implements PluginAuditEventTypes {
    public static final String SCHEDULER_JOB_CREATE = "scheduler:job:create";
    public static final String SCHEDULER_JOB_DELETE = "scheduler:job:delete";
    public static final String SCHEDULER_JOB_UPDATE = "scheduler:job:update";
    public static final String SCHEDULER_TRIGGER_CREATE = "scheduler:trigger:create";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
        .add(SCHEDULER_JOB_CREATE)
        .add(SCHEDULER_JOB_DELETE)
        .add(SCHEDULER_JOB_UPDATE)
        .add(SCHEDULER_TRIGGER_CREATE)
        .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
