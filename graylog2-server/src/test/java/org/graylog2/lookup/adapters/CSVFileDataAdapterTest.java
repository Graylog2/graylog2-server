/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.lookup.adapters;

import com.codahale.metrics.MetricRegistry;
import com.google.common.io.Resources;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class CSVFileDataAdapterTest {
    private final Path csvFile;
    private CSVFileDataAdapter csvFileDataAdapter;

    public CSVFileDataAdapterTest() throws Exception {
        final URL resource = Resources.getResource("org/graylog2/lookup/adapters/CSVFileDataAdapterTest.csv");
        this.csvFile = Paths.get(resource.toURI());
    }

    @Test
    public void doGet_successfully_returns_values() throws Exception {
        final CSVFileDataAdapter.Config config = CSVFileDataAdapter.Config.builder()
                .type(org.graylog2.lookup.adapters.CSVFileDataAdapter.NAME)
                .path(csvFile.toString())
                .separator(",")
                .quotechar("\"")
                .keyColumn("key")
                .valueColumn("value")
                .checkInterval(60)
                .caseInsensitiveLookup(false)
                .build();
        csvFileDataAdapter = new CSVFileDataAdapter("id", "name", config, new MetricRegistry());
        csvFileDataAdapter.doStart();

        assertThat(csvFileDataAdapter.doGet("foo")).isEqualTo(LookupResult.single("23"));
        assertThat(csvFileDataAdapter.doGet("bar")).isEqualTo(LookupResult.single("42"));
        assertThat(csvFileDataAdapter.doGet("quux")).isEqualTo(LookupResult.empty());
    }

    @Test
    public void doGet_successfully_returns_values_with_key_and_value_column_identical() throws Exception {
        final CSVFileDataAdapter.Config config = CSVFileDataAdapter.Config.builder()
                .type(org.graylog2.lookup.adapters.CSVFileDataAdapter.NAME)
                .path(csvFile.toString())
                .separator(",")
                .quotechar("\"")
                .keyColumn("key")
                .valueColumn("key")
                .checkInterval(60)
                .caseInsensitiveLookup(false)
                .build();
        csvFileDataAdapter = new CSVFileDataAdapter("id", "name", config, new MetricRegistry());
        csvFileDataAdapter.doStart();

        assertThat(csvFileDataAdapter.doGet("foo")).isEqualTo(LookupResult.single("foo"));
        assertThat(csvFileDataAdapter.doGet("bar")).isEqualTo(LookupResult.single("bar"));
        assertThat(csvFileDataAdapter.doGet("quux")).isEqualTo(LookupResult.empty());
    }
}