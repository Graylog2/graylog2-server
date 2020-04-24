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
package org.graylog.plugins.views.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class ViewsAuditEventTypes implements PluginAuditEventTypes {
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
    public static final String MESSAGES_EXPORT = PREFIX + SEARCH + ":export";

    private static final String SEARCH_JOB = "search_job";
    public static final String SEARCH_JOB_CREATE = PREFIX + SEARCH_JOB + ":create";


    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(VIEW_CREATE)
            .add(VIEW_UPDATE)
            .add(VIEW_DELETE)

            .add(DEFAULT_VIEW_SET)

            .add(SEARCH_CREATE)
            .add(SEARCH_EXECUTE)

            .add(MESSAGES_EXPORT)

            .add(SEARCH_JOB_CREATE)

            .add(VIEW_SHARING_CREATE)
            .add(VIEW_SHARING_DELETE)

            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
