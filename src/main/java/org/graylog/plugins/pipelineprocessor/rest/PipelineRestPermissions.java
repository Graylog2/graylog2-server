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
