package org.graylog2.featureflag;

import java.util.Map;

public interface FeatureFlags {
    Map<String,String> getAll();

    boolean isOn(String feature, boolean defaultValue);
}
