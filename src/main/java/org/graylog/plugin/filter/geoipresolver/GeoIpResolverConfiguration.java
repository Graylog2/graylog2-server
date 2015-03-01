package org.graylog.plugin.filter.geoipresolver;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.PluginConfigBean;

public class GeoIpResolverConfiguration implements PluginConfigBean {
    @Parameter(value = "geoip_resolver_database")
    private String geoIpDatabase = "/usr/local/share/GeoIp/GeoIP2-City.mmdb";

    @Parameter(value = "geoip_resolver_run_before_extractors")
    private boolean runBeforeExtractors = true;

    @Parameter(value = "geoip_resolver_enabled")
    private boolean enabled = false;

    public boolean isRunBeforeExtractors() {
        return runBeforeExtractors;
    }

    public String getGeoIpDatabase() {
        return geoIpDatabase;
    }

    public boolean isEnabled() {
        return enabled;
    }
}

