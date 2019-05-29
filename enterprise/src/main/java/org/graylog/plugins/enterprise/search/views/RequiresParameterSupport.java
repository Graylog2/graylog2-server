package org.graylog.plugins.enterprise.search.views;

import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.db.SearchDbService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class RequiresParameterSupport implements ViewRequirement {
    public static final String Parameters = "parameters";

    private final SearchDbService searchDbService;
    private final EnterpriseMetadataSummary enterpriseMetadataSummary;

    @Inject
    public RequiresParameterSupport(SearchDbService searchDbService, EnterpriseMetadataSummary enterpriseMetadataSummary) {
        this.searchDbService = searchDbService;
        this.enterpriseMetadataSummary = enterpriseMetadataSummary;
    }

    @Override
    public Map<String, PluginMetadataSummary> test(ViewDTO view) {
        final Optional<Search> optionalSearch = searchDbService.get(view.searchId());
        return optionalSearch.map(search -> search.parameters().isEmpty()
                ? Collections.<String, PluginMetadataSummary>emptyMap()
                : Collections.<String, PluginMetadataSummary>singletonMap(Parameters, enterpriseMetadataSummary))
                .orElseThrow(() -> new IllegalStateException("Search " + view.searchId() + " for view " + view + " is missing."));
    }
}
