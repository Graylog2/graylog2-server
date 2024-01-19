package org.graylog2.datatiering;

import org.graylog2.Configuration;
import org.graylog2.featureflag.FeatureFlags;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataTieringChecker {
    private final FeatureFlags featureFlags;
    private final Configuration configuration;

    @Inject
    public DataTieringChecker(FeatureFlags featureFlags, Configuration configuration) {
        this.featureFlags = featureFlags;
        this.configuration = configuration;
    }
    public static final String DATA_TIERING_CLOUD_FEATURE = "data_tiering_cloud";


    public boolean isEnabled() {
        boolean isCloud = configuration.isCloud();
        return !isCloud || featureFlags.isOn(DATA_TIERING_CLOUD_FEATURE);
    }
}
