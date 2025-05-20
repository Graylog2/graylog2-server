/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.web.customization;

import jakarta.inject.Inject;

import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Optional;


public class CustomizationConfig {
    private static final String DEFAULT_PRODUCT_NAME = "Graylog";
    private final Config config;
    private final Base64.Decoder base64Decoder;

    @Inject
    public CustomizationConfig(@Nullable Config config) {
        this.config = Optional.ofNullable(config).orElse(Config.empty());
        this.base64Decoder = Base64.getDecoder();
    }

    public String productName() {
        return config.productName().orElse(DEFAULT_PRODUCT_NAME);
    }

    public Optional<byte[]> favicon() {
        return config.favicon().map(base64Decoder::decode);
    }

    public static CustomizationConfig empty() {
        return new CustomizationConfig(Config.empty());
    }
}
