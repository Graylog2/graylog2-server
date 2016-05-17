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
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Collections;
import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class PipelineRestPermissions implements PluginPermissions {

    /* pipelines */
    public static final String PIPELINE_CREATE = "pipeline:create";
    public static final String PIPELINE_READ = "pipeline:read";
    public static final String PIPELINE_EDIT = "pipeline:edit";
    public static final String PIPELINE_DELETE = "pipeline:delete";

    /* rules */
    public static final String PIPELINE_RULE_CREATE = "pipeline_rule:create";
    public static final String PIPELINE_RULE_READ = "pipeline_rule:read";
    public static final String PIPELINE_RULE_EDIT = "pipeline_rule:edit";
    public static final String PIPELINE_RULE_DELETE = "pipeline_rule:delete";

    /* connections */
    public static final String PIPELINE_CONNECTION_READ = "pipeline_connection:read";
    public static final String PIPELINE_CONNECTION_EDIT = "pipeline_connection:edit";


    @Override
    public Set<Permission> permissions() {
        return ImmutableSet.of(
                create(PIPELINE_CREATE, "Create new processing pipeline"),
                create(PIPELINE_READ, "Read a processing pipeline"),
                create(PIPELINE_EDIT, "Update a processing pipeline"),
                create(PIPELINE_DELETE, "Delete a processing pipeline"),

                create(PIPELINE_RULE_CREATE, "Create new processing rule"),
                create(PIPELINE_RULE_READ, "Read a processing rule"),
                create(PIPELINE_RULE_EDIT, "Update a processing rule"),
                create(PIPELINE_RULE_DELETE, "Delete a processing rule"),

                create(PIPELINE_CONNECTION_READ, "Read a pipeline stream connection"),
                create(PIPELINE_CONNECTION_EDIT, "Update a pipeline stream connections")
                );
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Collections.emptySet();
    }
}
