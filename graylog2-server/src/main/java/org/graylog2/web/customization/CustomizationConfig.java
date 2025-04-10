package org.graylog2.web.customization;

import jakarta.inject.Inject;

public class CustomizationConfig {
    private static final String DEFAULT_PRODUCT_NAME = "Graylog";
    private final Config config;

    @Inject
    public CustomizationConfig(Config config) {
        this.config = config;
    }

    public String productName() {
        return config.productName().orElse(DEFAULT_PRODUCT_NAME);
    }
}
