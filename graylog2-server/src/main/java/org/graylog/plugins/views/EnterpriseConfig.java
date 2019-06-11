package org.graylog.plugins.views;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.PluginConfigBean;
import org.joda.time.Duration;

class EnterpriseConfig implements PluginConfigBean {
    private static final Duration DEFAULT_MAXIMUM_AGE_FOR_SEARCHES = Duration.standardDays(4);
    private static final String PREFIX = "enterprise_search_";
    private static final String MAX_SEARCH_AGE = PREFIX + "maximum_search_age";

    @Parameter(MAX_SEARCH_AGE)
    private Duration maxSearchAge = DEFAULT_MAXIMUM_AGE_FOR_SEARCHES;
}
