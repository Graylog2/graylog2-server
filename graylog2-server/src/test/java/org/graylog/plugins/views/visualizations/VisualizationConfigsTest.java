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
package org.graylog.plugins.views.visualizations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.views.search.views.widgets.aggregation.ColorScale;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class VisualizationConfigsTest {
    private static final Path FRONTEND_COLOR_SCALES = Path.of(
            "../graylog2-web-interface/src/views/logic/aggregationbuilder/visualizations/HeatmapVisualizationConfig.ts");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final VisualizationConfigs configs = new VisualizationConfigs(CoreVisualizations.all().stream()
            .collect(Collectors.toMap(VisualizationDescriptor::name, Function.identity())), objectMapper);

    @Test
    void describesEverySettingOfEveryVisualization() {
        final List<String> undescribed = new ArrayList<>();
        configs.list().stream()
                .filter(VisualizationDescriptor::hasSettings)
                .forEach(visualization -> collectUndescribed(visualization.name(),
                        configs.options(visualization.name()).settings(), undescribed));

        assertThat(undescribed).isEmpty();
        assertThat(configs.list()).allSatisfy(visualization -> assertThat(visualization.description()).isNotBlank());
    }

    @Test
    void listsVisualizationsWithoutSettings() {
        assertThat(configs.list()).extracting(VisualizationDescriptor::name)
                .containsExactly("area", "bar", "heatmap", "line", "map", "network", "numeric", "pie", "sankey",
                        "scatter", "table");
        assertThat(configs.options("pie").settings()).isNull();
        assertThat(configs.settingNames("sankey")).isEmpty();
        assertThat(configs.apply("pie", null, Map.of())).isNull();
        assertThatThrownBy(() -> configs.check("pie", Map.of("donut", true)))
                .hasMessage("visualization_config: pie has no settings");
    }

    @Test
    void describesSettingsWithTheirValuesAndDefaults() {
        final JsonNode settings = configs.options("numeric").settings();

        assertThat(configs.settingNames("numeric")).containsExactly("color_preference", "trend", "trend_preference");
        assertThat(settings.at("/properties/trend_preference/enum")).extracting(JsonNode::asText)
                .containsExactly("LOWER", "NEUTRAL", "HIGHER");
        assertThat(settings.at("/properties/trend/default").asBoolean()).isFalse();
        assertThat(settings.at("/additionalProperties").asBoolean(true)).isFalse();
        assertThat(settings.has("required")).isFalse();
        assertThat(configs.options("heatmap").settings().at("/properties/color_scale/enum")).extracting(JsonNode::asText)
                .contains("Viridis", "Cividis");
        assertThat(configs.options("bar").settings().at("/properties/axis_config/propertyNames/enum"))
                .extracting(JsonNode::asText).containsExactly("number", "percent", "time", "size", "xaxis");
    }

    @Test
    void hasDefaultsForEveryVisualizationWithSettings() {
        assertThat(configs.defaults("bar")).containsEntry("barmode", "group");
        assertThat(configs.defaults("map")).containsEntry("viewport", Map.of("center_x", 0.0, "center_y", 0.0, "zoom", 1));
        assertThat(configs.list().stream().filter(VisualizationDescriptor::hasSettings))
                .allSatisfy(visualization -> assertThat(configs.defaults(visualization.name())).isNotEmpty());
    }

    @Test
    void reportsEveryProblemByTheSettingTheToolGave() {
        assertThatThrownBy(() -> configs.check("numeric", Map.of("trend_preference", "UP", "trendd", true, "trend", "yes")))
                .isInstanceOf(InvalidVisualizationConfigException.class)
                .satisfies(e -> assertThat(((InvalidVisualizationConfigException) e).errors()).containsExactlyInAnyOrder(
                        "visualization_config.trend_preference: must be one of LOWER, NEUTRAL, HIGHER",
                        "visualization_config.trendd: numeric has no such setting; its settings are color_preference, "
                                + "trend, trend_preference",
                        "visualization_config.trend: must be of type boolean"));
        assertThatThrownBy(() -> configs.check("bar", Map.of("axis_config", Map.of("xaxis", Map.of("label", "Time")))))
                .hasMessage("visualization_config.axis_config.xaxis.label: no such setting");
        assertThatThrownBy(() -> configs.check("bar", Map.of("axis_config", Map.of("yaxis", Map.of("title", "Count")))))
                .hasMessage("visualization_config.axis_config.yaxis: no such key; the keys are number, percent, time, "
                        + "size, xaxis");
        assertThatThrownBy(() -> configs.check("heatmap", Map.of("color_scale", "Plasma")))
                .hasMessageStartingWith("visualization_config.color_scale: must be one of Greys, YlGnBu");
        assertThatThrownBy(() -> configs.check("donut", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Unknown visualization <donut>");
    }

    @Test
    void appliesChangesToTheDefaultsOfANewWidget() {
        assertThat(configs.apply("numeric", null, Map.of("trend", true, "trend_preference", "LOWER")))
                .containsEntry("trend", true)
                .containsEntry("trend_preference", "LOWER")
                .containsEntry("color_preference", "TREND_ONLY");
    }

    @Test
    void keepsPreviousSettingsTheChangesDoNotMention() {
        final Map<String, Object> previous = Map.of("barmode", "stack", "axis_type", "logarithmic",
                "axis_config", Map.of("number", Map.of("title", "Count"), "xaxis", Map.of("title", "Time")));

        final Map<String, Object> applied = configs.apply("bar", previous,
                Map.of("axis_config", Map.of("xaxis", Map.of("color", "#e6194b"))));

        assertThat(applied).containsEntry("barmode", "stack").containsEntry("axis_type", "logarithmic");
        assertThat(applied.get("axis_config")).asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("xaxis", Map.of("title", "Time", "color", "#e6194b"))
                .extractingByKey("number").asInstanceOf(InstanceOfAssertFactories.MAP).containsEntry("title", "Count");
    }

    @Test
    void resetsSettingsChangedToNull() {
        final Map<String, Object> changes = new HashMap<>();
        changes.put("z_min", null);
        changes.put("auto_scale", true);

        assertThat(configs.apply("heatmap", Map.of("color_scale", "Hot", "auto_scale", false, "z_min", "3"), changes))
                .containsEntry("color_scale", "Hot")
                .containsEntry("auto_scale", true)
                .doesNotContainEntry("z_min", "3");
    }

    @Test
    void keepsStoredValuesTheChangesDoNotTouch() {
        // Stored configs may hold values tools may not set, and settings only the web interface knows.
        final Map<String, Object> applied = configs.apply("heatmap",
                Map.of("color_scale", "Plasma", "reverse_scale", false, "legacy_option", 1), Map.of("reverse_scale", true));

        assertThat(applied).containsEntry("color_scale", "Plasma")
                .containsEntry("reverse_scale", true)
                .doesNotContainKey("legacy_option");
    }

    @Test
    void namesTypesInErrorsByTheirSimpleNames() {
        assertThat(VisualizationConfigs.withSimpleTypeNames("Cannot deserialize value of type "
                + "`org.graylog.Dto$TrendPreference` from String \"UP\": not one of `java.lang.String`"))
                .isEqualTo("Cannot deserialize value of type TrendPreference from String \"UP\": not one of String");
    }

    @Test
    void namesTypesWithoutBacktrackingOnUnclosedQuotes() {
        final String unclosed = "`" + "a$".repeat(50_000) + "!";

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> VisualizationConfigs.withSimpleTypeNames(unclosed));
    }

    @Test
    void offersTheColorScalesOfTheWebInterface() throws IOException {
        final String source = Files.readString(FRONTEND_COLOR_SCALES);
        final String list = Pattern.compile("export const COLORSCALES = \\[(.*?)]", Pattern.DOTALL).matcher(source)
                .results().findFirst().orElseThrow().group(1);
        final List<String> frontend = Pattern.compile("'([^']+)'").matcher(list).results().map(m -> m.group(1)).toList();

        assertThat(Arrays.stream(ColorScale.values()).map(Enum::name).toList())
                .as("ColorScale must list the color scales of COLORSCALES in %s", FRONTEND_COLOR_SCALES)
                .containsExactlyElementsOf(frontend);
    }

    private static void collectUndescribed(String path, JsonNode schema, List<String> undescribed) {
        schema.path("properties").properties().forEach(entry -> {
            final String property = path + "." + entry.getKey();
            if (!entry.getValue().hasNonNull("description")) {
                undescribed.add(property);
            }
            collectUndescribed(property, entry.getValue(), undescribed);
            collectUndescribed(property + ".*", entry.getValue().path("additionalProperties"), undescribed);
        });
    }
}
