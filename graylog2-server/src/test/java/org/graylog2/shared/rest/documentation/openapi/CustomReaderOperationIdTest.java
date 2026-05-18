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
package org.graylog2.shared.rest.documentation.openapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomReaderOperationIdTest {

    @Test
    void corePackageGetsNoPrefix() {
        assertThat(CustomReader.derivePluginPrefix("org.graylog2.rest.resources.streams")).isEmpty();
        assertThat(CustomReader.derivePluginPrefix("org.graylog2.shared.rest")).isEmpty();
    }

    @Test
    void pluginPackageGetsCapitalizedFirstSegmentAfterPlugins() {
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins.sidecar.rest.resources"))
                .isEqualTo("Sidecar");
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins.views.search.rest"))
                .isEqualTo("Views");
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins.securityapp.investigations.rest"))
                .isEqualTo("Securityapp");
    }

    @Test
    void packageWithoutPluginsSegmentGetsNoPrefix() {
        assertThat(CustomReader.derivePluginPrefix("com.example.something")).isEmpty();
    }

    @Test
    void packageEndingInPluginsGetsNoPrefix() {
        // "plugins" is the last segment, no segment follows
        assertThat(CustomReader.derivePluginPrefix("org.graylog.plugins")).isEmpty();
    }

    @Test
    void stripsTrailingResourceFromClassName() {
        assertThat(CustomReader.stripResourceSuffix("StreamResource")).isEqualTo("Stream");
        assertThat(CustomReader.stripResourceSuffix("ConfigurationResource")).isEqualTo("Configuration");
    }

    @Test
    void leavesClassNamesNotEndingInResourceUntouched() {
        assertThat(CustomReader.stripResourceSuffix("TestResource2")).isEqualTo("TestResource2");
        assertThat(CustomReader.stripResourceSuffix("Foo")).isEqualTo("Foo");
        assertThat(CustomReader.stripResourceSuffix("Resources")).isEqualTo("Resources");
    }

    @Test
    void stripsResourceSuffixOnlyFromTheEnd() {
        // "Resource" appearing in the middle isn't stripped
        assertThat(CustomReader.stripResourceSuffix("ResourceFoo")).isEqualTo("ResourceFoo");
    }

    @Test
    void normalizesCamelCaseParam() {
        assertThat(CustomReader.normalizeParamName("streamId")).isEqualTo("StreamId");
        assertThat(CustomReader.normalizeParamName("name")).isEqualTo("Name");
    }

    @Test
    void normalizesSnakeCaseParam() {
        assertThat(CustomReader.normalizeParamName("index_set_id")).isEqualTo("IndexSetId");
        assertThat(CustomReader.normalizeParamName("per_page")).isEqualTo("PerPage");
    }

    @Test
    void preservesAlreadyCapitalizedParam() {
        assertThat(CustomReader.normalizeParamName("StreamId")).isEqualTo("StreamId");
    }

    @Test
    void normalizesParamWithHyphens() {
        assertThat(CustomReader.normalizeParamName("content-type")).isEqualTo("ContentType");
    }

    @Test
    void normalizesParamWithSpaces() {
        assertThat(CustomReader.normalizeParamName(" foo bar ")).isEqualTo("FooBar");
    }

    @Test
    void doesNotLowercaseInternalCaps() {
        // XMLContent must not become Xmlcontent
        assertThat(CustomReader.normalizeParamName("XMLContent")).isEqualTo("XMLContent");
    }

    @Test
    void buildsEmptySuffixForNoParams() {
        assertThat(CustomReader.paramSuffix(List.of())).isEmpty();
        assertThat(CustomReader.paramSuffix(null)).isEmpty();
    }

    @Test
    void buildsBySuffixForOneParam() {
        assertThat(CustomReader.paramSuffix(List.of("streamId"))).isEqualTo("ByStreamId");
    }

    @Test
    void buildsByAndSuffixForMultipleParams() {
        assertThat(CustomReader.paramSuffix(List.of("streamId", "page", "per_page")))
                .isEqualTo("ByStreamIdAndPageAndPerPage");
    }

    @Test
    void buildsBySuffixUsingActualParameterOrder() {
        // The param order from the JAX-RS method is preserved, not sorted
        assertThat(CustomReader.paramSuffix(List.of("zebra", "alpha")))
                .isEqualTo("ByZebraAndAlpha");
    }
}
