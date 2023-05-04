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
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import org.graylog2.lookup.AllowedAuxiliaryPathChecker;
import org.graylog2.plugin.lookup.LookupCachePurge;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.graylog2.lookup.adapters.CSVFileDataAdapter.Config;
import static org.graylog2.lookup.adapters.CSVFileDataAdapter.NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CSVFileDataAdapterTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private final Path csvFile;
    private final Path cidrLookupFile;
    private CSVFileDataAdapter csvFileDataAdapter;

    @Mock
    AllowedAuxiliaryPathChecker pathChecker;

    @Mock
    private LookupCachePurge cachePurge;

    @Mock
    private LookupDataAdapterValidationContext validationContext;

    public CSVFileDataAdapterTest() throws Exception {
        final URL resource = Resources.getResource("org/graylog2/lookup/adapters/CSVFileDataAdapterTest.csv");
        final Path csvFilePath = Paths.get(resource.toURI());
        this.csvFile = csvFilePath;

        final URL cidrResource = Resources.getResource("org/graylog2/lookup/adapters/CSVFileDataAdapterCIDRLookupTest.csv");
        final Path cidrLookupFilePath = Paths.get(cidrResource.toURI());
        this.cidrLookupFile = cidrLookupFilePath;
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
                .hasMessageStartingWith(CSVFileDataAdapter.ALLOWED_PATH_ERROR);
    }

    @Test
    public void refresh_success() throws Exception {
        final Config config = baseConfig();
        csvFileDataAdapter = spy(new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker));
        when(pathChecker.fileIsInAllowedPath(isA(Path.class))).thenReturn(true).thenReturn(true);
        csvFileDataAdapter.doStart();
        csvFileDataAdapter.doRefresh(cachePurge);
        assertFalse(csvFileDataAdapter.getError().isPresent());
    }

    @Test
    public void refresh_failure_disallowedFileLocation() throws Exception {
        final Config config = baseConfig();
        csvFileDataAdapter = spy(new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker));
        when(pathChecker.fileIsInAllowedPath(isA(Path.class))).thenReturn(true).thenReturn(false);
        csvFileDataAdapter.doStart();
        csvFileDataAdapter.doRefresh(cachePurge);
        assertTrue(csvFileDataAdapter.getError().isPresent());
    }

    /**
     * Verify recovery after refresh failure due to file in disallowed location.
     */
    @Test
    public void refresh_failure_success() throws Exception {
        final Config config = baseConfig();
        csvFileDataAdapter = spy(new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker));
        when(pathChecker.fileIsInAllowedPath(isA(Path.class))).thenReturn(true).thenReturn(false);
        csvFileDataAdapter.doStart();
        csvFileDataAdapter.doRefresh(cachePurge);
        assertTrue(csvFileDataAdapter.getError().isPresent());
        when(pathChecker.fileIsInAllowedPath(isA(Path.class))).thenReturn(true).thenReturn(true);
        csvFileDataAdapter.doRefresh(cachePurge);
        assertThat(csvFileDataAdapter.doGet("foo")).isEqualTo(LookupResult.single("23"));
    }

    @Test
    public void testConfigValidationSuccess() {
        final Config config = Config.builder()
                                    .type(NAME)
                                    .path(csvFile.toString())
                                    .separator(",")
                                    .quotechar("\"")
                                    .keyColumn("key")
                                    .valueColumn("value")
                                    .checkInterval(60)
                                    .caseInsensitiveLookup(false)
                                    .build();
        when(validationContext.getPathChecker()).thenReturn(pathChecker);
        when(pathChecker.fileIsInAllowedPath(any(Path.class))).thenReturn(true);
        final Optional<Multimap<String, String>> result = config.validate(validationContext);
        assertFalse(result.isPresent());
    }

    @Test
    public void testConfigValidationFileDoesNotExist() {
        final Config config = Config.builder()
                                    .type(NAME)
                                    .path("fake-path")
                                    .separator(",")
                                    .quotechar("\"")
                                    .keyColumn("key")
                                    .valueColumn("value")
                                    .checkInterval(60)
                                    .caseInsensitiveLookup(false)
                                    .build();
        when(validationContext.getPathChecker()).thenReturn(pathChecker);
        when(pathChecker.fileIsInAllowedPath(any(Path.class))).thenReturn(true);
        final Optional<Multimap<String, String>> result = config.validate(validationContext);
        assertTrue(result.isPresent());
        assertEquals("The file does not exist.", String.join("", result.get().asMap().get("path")));
    }

    @Test
    public void testConfigValidationFileNotInPermittedLocation() {
        final Config config = Config.builder()
                .type(NAME)
                .path("fake-path")
                .separator(",")
                .quotechar("\"")
                .keyColumn("key")
                .valueColumn("value")
                .checkInterval(60)
                .caseInsensitiveLookup(false)
                .build();
        when(validationContext.getPathChecker()).thenReturn(pathChecker);
        when(pathChecker.fileIsInAllowedPath(any(Path.class))).thenReturn(false);
        final Optional<Multimap<String, String>> result = config.validate(validationContext);
        assertTrue(result.isPresent());
        assertEquals(CSVFileDataAdapter.ALLOWED_PATH_ERROR,
                String.join("", result.get().asMap().get("path")));
    }

    @Test
    public void testCIDRLookups() throws Exception {
        final Config config = cidrLookupConfig();
        csvFileDataAdapter = spy(new CSVFileDataAdapter("id", "name", config, new MetricRegistry(), pathChecker));
        when(pathChecker.fileIsInAllowedPath((isA(Path.class)))).thenReturn(true);
        csvFileDataAdapter.doStart();

        assertThat(csvFileDataAdapter.doGet("10.10.64.128")).isEqualTo(LookupResult.single("Corporate"));
        assertThat(csvFileDataAdapter.doGet("192.168.100.112")).isEqualTo(LookupResult.single("Finance"));
        assertThat(csvFileDataAdapter.doGet("192.168.101.66")).isEqualTo(LookupResult.single("IT"));
        assertThat(csvFileDataAdapter.doGet("192.168.102.205")).isEqualTo(LookupResult.single("HR"));
        assertThat(csvFileDataAdapter.doGet("192.168.102.8")).isEqualTo(LookupResult.single("HR Subnet 1"));
        assertThat(csvFileDataAdapter.doGet("192.168.102.20")).isEqualTo(LookupResult.single("HR Subnet 2"));
        assertThat(csvFileDataAdapter.doGet("192.168.102.33")).isEqualTo(LookupResult.single("HR Subnet 3"));
        assertThat(csvFileDataAdapter.doGet("192.168.102.48")).isEqualTo(LookupResult.single("HR Subnet 4"));
        assertThat(csvFileDataAdapter.doGet("192.168.102.79")).isEqualTo(LookupResult.single("HR Subnet 5"));
        assertThat(csvFileDataAdapter.doGet("8.8.8.8")).isEqualTo(LookupResult.single("Google DNS"));
        assertThat(csvFileDataAdapter.doGet("2001:db7::")).isEqualTo(LookupResult.single("Single IPv6"));
        assertThat(csvFileDataAdapter.doGet("2002:0000:0000:1234:abcd:1234:4321:dcba")).isEqualTo(LookupResult.single("IPv6 Range"));
        assertThat(csvFileDataAdapter.doGet("192.168.103.16")).isEqualTo(LookupResult.empty());
        assertThat(csvFileDataAdapter.doGet("not.an.ip.address")).isEqualTo(LookupResult.withError());
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

    private Config cidrLookupConfig() {
        return Config.builder()
                .type(NAME)
                .path(cidrLookupFile.toString())
                .separator(",")
                .quotechar("\"")
                .keyColumn("key")
                .valueColumn("value")
                .checkInterval(60)
                .caseInsensitiveLookup(false)
                .cidrLookup(true)
                .build();
    }
}
