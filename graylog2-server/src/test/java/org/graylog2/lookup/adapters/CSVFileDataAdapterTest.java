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
package org.graylog2.lookup.adapters;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Resources;
import org.graylog2.lookup.AllowedAuxiliaryPathChecker;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.graylog2.lookup.adapters.CSVFileDataAdapter.Config;
import static org.graylog2.lookup.adapters.CSVFileDataAdapter.NAME;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CSVFileDataAdapterTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private final Path csvFile;
    private CSVFileDataAdapter csvFileDataAdapter;

    @Mock
    AllowedAuxiliaryPathChecker pathChecker;

    public CSVFileDataAdapterTest() throws Exception {
        final URL resource = Resources.getResource("org/graylog2/lookup/adapters/CSVFileDataAdapterTest.csv");
        final Path csvFilePath = Paths.get(resource.toURI());
        this.csvFile = csvFilePath;
    }

    @Test
    public void doGet_successfully_returns_values() throws Exception {
        final Config config = baseConfig();
        csvFileDataAdapter = spy(new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker));
        when(pathChecker.fileIsInAllowedPath(isA(Path.class))).thenReturn(true);
        csvFileDataAdapter.doStart();

        assertThat(csvFileDataAdapter.doGet("foo")).isEqualTo(LookupResult.single("23"));
        assertThat(csvFileDataAdapter.doGet("bar")).isEqualTo(LookupResult.single("42"));
        assertThat(csvFileDataAdapter.doGet("quux")).isEqualTo(LookupResult.empty());
    }

    @Test
    public void doGet_successfully_returns_values_with_key_and_value_column_identical() throws Exception {
        final Config config = Config.builder()
                                    .type(NAME)
                                    .path(csvFile.toString())
                                    .separator(",")
                                    .quotechar("\"")
                                    .keyColumn("key")
                                    .valueColumn("key")
                                    .checkInterval(60)
                                    .caseInsensitiveLookup(false)
                                    .build();
        csvFileDataAdapter = spy(new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker));
        when(pathChecker.fileIsInAllowedPath((isA(Path.class)))).thenReturn(true);
        csvFileDataAdapter.doStart();

        assertThat(csvFileDataAdapter.doGet("foo")).isEqualTo(LookupResult.single("foo"));
        assertThat(csvFileDataAdapter.doGet("bar")).isEqualTo(LookupResult.single("bar"));
        assertThat(csvFileDataAdapter.doGet("quux")).isEqualTo(LookupResult.empty());
    }

    @Test
    public void doGet_failure_filePathInvalid() throws Exception {
        final Config config = baseConfig();
        when(pathChecker.fileIsInAllowedPath((isA(Path.class)))).thenReturn(false);
        csvFileDataAdapter = new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker);
        assertThatThrownBy(() -> csvFileDataAdapter.doStart())
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("The specified CSV file is not in an allowed path.");
    }

    private Config baseConfig() {
        return Config.builder()
                     .type(NAME)
                     .path(csvFile.toString())
                     .separator(",")
                     .quotechar("\"")
                     .keyColumn("key")
                     .valueColumn("value")
                     .checkInterval(60)
                     .caseInsensitiveLookup(false)
                     .build();
    }
}
