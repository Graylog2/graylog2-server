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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;


@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record Config(
        @JsonProperty("product_name") Optional<String> productName,
        Optional<String> favicon,
        Optional<Logo> logo,
        @JsonProperty("help_url") Optional<String> helpUrl,
        Optional<Login> login,
        Optional<Welcome> welcome,
        Optional<Navigation> navigation,
        Optional<Footer> footer,
        Optional<Resources> resources
) {
    public record Logo(SVG light, SVG dark) {}
    public record Login(Optional<SVG> background) {}

    public record Welcome(Optional<WelcomeItem> news,
                          Optional<WelcomeItem> releases) {
        public record WelcomeItem(Optional<Boolean> enabled, Optional<String> feed) {}
    }

    public record Resources(@JsonProperty("stream_rule_matcher_code") Optional<ResourceItem> streamRuleMatcherCode,
                            @JsonProperty("enterprise_product") Optional<ResourceItem> enterpriseProduct,
                            @JsonProperty("contact_sales") Optional<ResourceItem> contactSales,
                            @JsonProperty("contact_support") Optional<ResourceItem> contactSupport,
                            @JsonProperty("contact_us") Optional<ResourceItem> contactUs
                            ) {
        public record ResourceItem(Optional<Boolean> enabled, Optional<String> feed) {}
    }

    public record Navigation(Optional<NavigationItem> home,
                             @JsonProperty("user_menu") Optional<NavigationItem> userMenu,
                             Optional<NavigationItem> scratchpad,
                             Optional<NavigationItem> help) {
        public record NavigationItem(String icon) {}
    }

    public record Footer(Optional<Boolean> enabled) {}

    public static Config empty() {
        return new Config(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
