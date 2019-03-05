package org.graylog.plugins.enterprise.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class EnterpriseAuditEventTypes implements PluginAuditEventTypes {
    public static final String NAMESPACE = "views";
    private static final String PREFIX = NAMESPACE + ":";

    private static final String VIEW = "view";
    public static final String VIEW_CREATE = PREFIX + VIEW + ":create";
    public static final String VIEW_UPDATE = PREFIX + VIEW + ":update";
    public static final String VIEW_DELETE = PREFIX + VIEW + ":delete";

    private static final String VIEW_SHARING = "view_sharing";
    public static final String VIEW_SHARING_CREATE = PREFIX + VIEW_SHARING + ":create";
    public static final String VIEW_SHARING_DELETE = PREFIX + VIEW_SHARING + ":delete";
    
    private static final String DEFAULT_VIEW = "default_view";
    public static final String DEFAULT_VIEW_SET = PREFIX + DEFAULT_VIEW + ":set";

    private static final String SEARCH = "search";
    public static final String SEARCH_CREATE = PREFIX + SEARCH + ":create";
    public static final String SEARCH_EXECUTE = PREFIX + SEARCH + ":execute";

    private static final String SEARCH_JOB = "search_job";
    public static final String SEARCH_JOB_CREATE = PREFIX + SEARCH_JOB + ":create";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(VIEW_CREATE)
            .add(VIEW_UPDATE)
            .add(VIEW_DELETE)

            .add(DEFAULT_VIEW_SET)

            .add(SEARCH_CREATE)
            .add(SEARCH_EXECUTE)

            .add(SEARCH_JOB_CREATE)

            .add(VIEW_SHARING_CREATE)
            .add(VIEW_SHARING_DELETE)

            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
