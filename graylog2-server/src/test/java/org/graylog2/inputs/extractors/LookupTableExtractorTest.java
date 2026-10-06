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
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.ConfigurationException;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class LookupTableExtractorTest {
    private static final String LUT_NAME = "my-lookup-table";

    @Mock
    private LookupTableService lookupTableService;
    @Mock
    private LookupTableService.Builder builder;
    @Mock
    private LookupTableService.Function function;

    @BeforeEach
    public void setUp() {
        lenient().when(lookupTableService.newBuilder()).thenReturn(builder);
        lenient().when(builder.lookupTable(LUT_NAME)).thenReturn(builder);
        lenient().when(builder.build()).thenReturn(function);
    }

    private LookupTableExtractor buildExtractor() throws Extractor.ReservedFieldException, ConfigurationException {
        return new LookupTableExtractor(new MetricRegistry(), lookupTableService, "id", "title", 0L,
                Extractor.CursorStrategy.COPY, "source", "target",
                Map.of(LookupTableExtractor.CONFIG_LUT_NAME, LUT_NAME), "user",
                Collections.<Converter>emptyList(), Extractor.ConditionType.NONE, "");
    }

    @Test
    public void constructorSucceedsWhenLookupTableIsMissing() throws Exception {
        // The lookup table no longer exists (e.g. it was deleted after the extractor was created). Construction must
        // still succeed so the extractor stays listable, editable and deletable. See issue #26122. Note that we do not
        // stub hasTable() at all - it must no longer be consulted during construction.
        assertThat(buildExtractor()).isNotNull();
    }

    @Test
    public void runReturnsNoResultWhenLookupTableIsMissing() throws Exception {
        // At runtime the missing table is resolved to an error result, which the extractor must handle gracefully.
        when(function.lookup(any())).thenReturn(LookupResult.withError());

        final LookupTableExtractor extractor = buildExtractor();

        assertThat(extractor.run("anything")).isNull();
    }

    @Test
    public void runExtractsSingleValue() throws Exception {
        when(function.lookup("anything")).thenReturn(LookupResult.single("resolved"));

        final LookupTableExtractor extractor = buildExtractor();

        assertThat(extractor.run("anything"))
                .containsExactly(new Extractor.Result("resolved", "target", -1, -1));
    }

    @Test
    public void constructorFailsWhenLookupTableNameIsMissing() {
        assertThatThrownBy(() -> new LookupTableExtractor(new MetricRegistry(), lookupTableService, "id", "title", 0L,
                Extractor.CursorStrategy.COPY, "source", "target", Map.of(), "user",
                Collections.<Converter>emptyList(), Extractor.ConditionType.NONE, ""))
                .isInstanceOf(ConfigurationException.class);
    }
}
