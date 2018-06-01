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
/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.sidecar.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class SidecarAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "sidecar:";

    public static final String ACTION_UPDATE = NAMESPACE + "action:update";

    public static final String SIDECAR_UPDATE = NAMESPACE + "sidecar:update";
    public static final String SIDECAR_DELETE = NAMESPACE + "sidecar:delete";

    public static final String COLLECTOR_CREATE = NAMESPACE + "collector:create";
    public static final String COLLECTOR_UPDATE = NAMESPACE + "collector:update";
    public static final String COLLECTOR_DELETE = NAMESPACE + "collector:delete";
    public static final String COLLECTOR_CLONE = NAMESPACE + "collector:clone";

    public static final String CONFIGURATION_CREATE = NAMESPACE + "configuration:create";
    public static final String CONFIGURATION_UPDATE = NAMESPACE + "configuration:update";
    public static final String CONFIGURATION_DELETE = NAMESPACE + "configuration:delete";
    public static final String CONFIGURATION_CLONE = NAMESPACE + "configuration:clone";

    private static final Set<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(ACTION_UPDATE)
            .add(SIDECAR_UPDATE)
            .add(SIDECAR_DELETE)
            .add(COLLECTOR_CREATE)
            .add(COLLECTOR_UPDATE)
            .add(COLLECTOR_DELETE)
            .add(COLLECTOR_CLONE)
            .add(CONFIGURATION_CREATE)
            .add(CONFIGURATION_UPDATE)
            .add(CONFIGURATION_DELETE)
            .add(CONFIGURATION_CLONE)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
