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

import java.util.Optional;


@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record Config(
        Optional<String> productName,
        Optional<String> favicon,
        Optional<Logo> logo,
        Optional<String> helpUrl,
        Optional<Login> login,
        Optional<Welcome> welcome,
        Optional<Navigation> navigation,
        Optional<Footer> footer
) {
    public record Logo(SVG light, SVG dark) {}
    public record Login(Optional<SVG> background) {}

    public record Welcome(Optional<WelcomeItem> news,
                          Optional<WelcomeItem> releases) {
        public record WelcomeItem(Optional<Boolean> enabled, Optional<String> feed) {}
    }

    public record Navigation(Optional<NavigationItem> home,
                             Optional<NavigationItem> userMenu,
                             Optional<NavigationItem> scratchpad,
                             Optional<NavigationItem> help) {
        public record NavigationItem(String icon) {}
    }

    public record Footer(Optional<Boolean> enabled) {}

    public static Config empty() {
        return new Config(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
