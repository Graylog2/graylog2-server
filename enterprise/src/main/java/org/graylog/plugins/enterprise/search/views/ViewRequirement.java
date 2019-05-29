package org.graylog.plugins.enterprise.search.views;

import java.util.Map;

public interface ViewRequirement {
    Map<String, PluginMetadataSummary> test(ViewDTO view);
}
