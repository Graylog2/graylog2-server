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
package org.graylog.plugins.pipelineprocessor.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class PipelineProcessorAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "pipeline_processor:";

    public static final String PIPELINE_CONNECTION_UPDATE = NAMESPACE + "pipeline_connection:update";
    public static final String PIPELINE_CREATE = NAMESPACE + "pipeline:create";
    public static final String PIPELINE_UPDATE = NAMESPACE + "pipeline:update";
    public static final String PIPELINE_DELETE = NAMESPACE + "pipeline:delete";
    public static final String RULE_CREATE = NAMESPACE + "rule:create";
    public static final String RULE_UPDATE = NAMESPACE + "rule:update";
    public static final String RULE_DELETE = NAMESPACE + "rule:delete";

    private static final Set<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(PIPELINE_CONNECTION_UPDATE)
            .add(PIPELINE_CREATE)
            .add(PIPELINE_UPDATE)
            .add(PIPELINE_DELETE)
            .add(RULE_CREATE)
            .add(RULE_UPDATE)
            .add(RULE_DELETE)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
