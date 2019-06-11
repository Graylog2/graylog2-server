package org.graylog.plugins.views;

import org.graylog.plugins.views.search.views.PluginMetadataSummary;

import java.util.Map;

public interface Requirement<O> {
    Map<String, PluginMetadataSummary> test(O dto);
}
