package org.graylog.plugins.enterprise;

import org.graylog.plugins.enterprise.search.views.PluginMetadataSummary;

import java.util.Map;

public interface Requirement<O> {
    Map<String, PluginMetadataSummary> test(O dto);
}
