package org.graylog.plugins.enterprise.search;

import org.graylog.plugins.enterprise.Requirement;
import org.graylog.plugins.enterprise.search.views.EnterpriseMetadataSummary;
import org.graylog.plugins.enterprise.search.views.PluginMetadataSummary;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

public class SearchRequiresParameterSupport implements Requirement<Search> {
    public static final String Parameters = "parameters";

    private final EnterpriseMetadataSummary enterpriseMetadataSummary;

    @Inject
    public SearchRequiresParameterSupport(EnterpriseMetadataSummary enterpriseMetadataSummary) {
        this.enterpriseMetadataSummary = enterpriseMetadataSummary;
    }

    @Override
    public Map<String, PluginMetadataSummary> test(Search search) {
        return search.parameters().isEmpty()
                ? Collections.emptyMap()
                : Collections.singletonMap(Parameters, enterpriseMetadataSummary);
    }
}
