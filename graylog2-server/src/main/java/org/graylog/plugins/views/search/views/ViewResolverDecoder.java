package org.graylog.plugins.views.search.views;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides support for decoding resolver view IDs (in the format resolver-name:viewId).
 * See {@link ViewResolver} for more information.
 */
public class ViewResolverDecoder {

    public static final String SEPARATOR = ":";
    private final String viewId;

    public ViewResolverDecoder(String viewId) throws IllegalArgumentException {
        Preconditions.checkArgument(StringUtils.isNotBlank(viewId), "View ID cannot be blank.");
        this.viewId = viewId;
    }

    /**
     * @return Indicates if the view ID provided is a resolver id or a standard hex view id.
     */
    public boolean isResolverViewId() {
        return viewId.contains(SEPARATOR);
    }

    /**
     * @return The resolver name (provided before the ':' separator).
     */
    public String getResolverName() {
        final String[] split = viewId.split(SEPARATOR);
        Preconditions.checkArgument(split.length == 2, "Cannot get resolver name for standard view ID.");
        return split[0];
    }

    /**
     * @return The view id (provided after the ':' separator).
     */
    public String getViewId() {
        final String[] split = viewId.split(SEPARATOR);
        Preconditions.checkArgument(split.length == 2, "Cannot get resolver ID for standard view ID.");
        return split[1];
    }
}
