package org.graylog.plugins.views.search.views;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

public class ViewResolverDecoder {

    public static final String SEPARATOR = ":";
    private final String viewId;

    public ViewResolverDecoder(String viewId) throws IllegalArgumentException {
        Preconditions.checkArgument(StringUtils.isNotBlank(viewId), "View ID cannot be blank.");
        this.viewId = viewId;
    }

    public boolean isResolverId() {
        return viewId.contains(SEPARATOR);
    }

    public String getResolverName() {
        final String[] split = viewId.split(SEPARATOR);
        Preconditions.checkArgument(split.length == 2, "Cannot get resolver name for standard view ID.");
        return split[0];
    }

    public String getViewId() {
        final String[] split = viewId.split(SEPARATOR);
        Preconditions.checkArgument(split.length == 2, "Cannot get resolver ID for standard view ID.");
        return split[1];
    }
}
