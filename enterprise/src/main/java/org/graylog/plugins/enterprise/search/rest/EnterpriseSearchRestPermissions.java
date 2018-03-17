package org.graylog.plugins.enterprise.search.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Collections;
import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class EnterpriseSearchRestPermissions implements PluginPermissions {
    public static final String VIEW_CREATE = "view:create";
    public static final String VIEW_READ = "view:read";
    public static final String VIEW_EDIT = "view:edit";
    public static final String VIEW_DELETE = "view:delete";
    public static final String DEFAULT_VIEW_SET = "default-view:set";

    private final ImmutableSet<Permission> permissions = ImmutableSet.of(
            create(VIEW_CREATE, "Create new view"),
            create(VIEW_READ, "Read available views"),
            create(VIEW_EDIT, "Edit view"),
            create(VIEW_DELETE, "Delete view"),
            create(DEFAULT_VIEW_SET, "Set default view")
    );

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Collections.emptySet();
    }
}
