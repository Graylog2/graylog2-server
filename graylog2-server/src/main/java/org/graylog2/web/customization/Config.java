package org.graylog2.web.customization;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Optional;


@JsonInclude(JsonInclude.Include.NON_ABSENT)
public record Config(
        Optional<String> productName,
        Optional<String> favicon,
        Optional<SVG> logo,
        Optional<String> helpUrl,
        Optional<Login> login,
        Optional<Welcome> welcome,
        Optional<Navigation> navigation,
        Optional<Footer> footer
) {
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
}
