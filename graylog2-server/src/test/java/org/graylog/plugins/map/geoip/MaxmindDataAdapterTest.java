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
package org.graylog.plugins.map.geoip;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import org.graylog.plugins.map.ConditionalRunner;
import org.graylog.plugins.map.ResourceExistsCondition;
import org.graylog.plugins.map.config.DatabaseType;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MaxmindDataAdapterTest.CityDatabaseTest.class,
        MaxmindDataAdapterTest.CountryDatabaseTest.class
})
public class MaxmindDataAdapterTest {
    private static final String GEO_LITE2_CITY_MMDB = "/GeoLite2-City.mmdb";
    private static final String GEO_LITE2_COUNTRY_MMDB = "/GeoLite2-Country.mmdb";

    private static final Map<DatabaseType, String> DB_PATH = ImmutableMap.of(
            DatabaseType.MAXMIND_CITY, GEO_LITE2_CITY_MMDB,
            DatabaseType.MAXMIND_COUNTRY, GEO_LITE2_COUNTRY_MMDB
    );

    abstract static class Base {
        MaxmindDataAdapter adapter;
        private final DatabaseType databaseType;

        Base(DatabaseType databaseType) {
            this.databaseType = databaseType;
        }

        @Before
        public void setUp() throws Exception {
            final MaxmindDataAdapter.Config config = MaxmindDataAdapter.Config.builder()
                    .checkInterval(1L)
                    .checkIntervalUnit(TimeUnit.HOURS)
                    .dbType(databaseType)
                    .path(getTestDatabasePath(DB_PATH.get(databaseType)))
                    .type("test")
                    .build();
            adapter = new MaxmindDataAdapter("test", "test", config, new MetricRegistry());

            adapter.doStart();
        }

        @After
        public void tearDown() throws Exception {
            adapter.doStop();
        }

        private String getTestDatabasePath(String name) throws URISyntaxException {
            return this.getClass().getResource(name).toURI().getPath();
        }

        @Test
        public void doGetSuccessfullyResolvesLoopBackToEmptyResult() {
            final LookupResult lookupResult = adapter.doGet("127.0.0.1");
            assertThat(lookupResult.isEmpty()).isTrue();
        }

        @Test
        public void doGetSuccessfullyResolvesRFC1918AddressToEmptyResult() {
            final LookupResult lookupResult = adapter.doGet("192.168.23.42");
            assertThat(lookupResult.isEmpty()).isTrue();
        }

        @Test
        public void doGetReturnsEmptyResultIfDatabaseReaderReturnsNull() {
            final DatabaseReader mockDatabaseReader = mock(DatabaseReader.class);
            final DatabaseReader oldDatabaseReader = adapter.getDatabaseReader();

            try {
                adapter.setDatabaseReader(mockDatabaseReader);

                final LookupResult lookupResult = adapter.doGet("127.0.0.1");
                assertThat(lookupResult.isEmpty()).isTrue();
            } finally {
                adapter.setDatabaseReader(oldDatabaseReader);
            }
        }

        @Test
        public void doGetReturnsEmptyResultForInvalidIPAddress() {
            final LookupResult lookupResult = adapter.doGet("Foobar");
            assertThat(lookupResult.isEmpty()).isTrue();
        }
    }

    @RunWith(ConditionalRunner.class)
    @ResourceExistsCondition(GEO_LITE2_CITY_MMDB)
    public static class CityDatabaseTest extends Base {
        public CityDatabaseTest() {
            super(DatabaseType.MAXMIND_CITY);
        }

        @Test
        public void doGetSuccessfullyResolvesGooglePublicDNSAddress() {
            // This test will possibly get flaky when the entry for 8.8.8.8 changes!
            final LookupResult lookupResult = adapter.doGet("8.8.8.8");
            assertThat(lookupResult.isEmpty()).isFalse();
            assertThat(lookupResult.multiValue())
                    .extracting("city")
                    .extracting("geoNameId")
                    .containsExactly(5375480);
            assertThat(lookupResult.multiValue())
                    .extracting("location")
                    .extracting("metroCode")
                    .containsExactly(807);
        }

        @Test
        public void doGetIncludesCoordinatesInMultiValueResult() {
            // This test will possibly get flaky when the entry for 8.8.8.8 changes!
            final LookupResult lookupResult = adapter.doGet("8.8.8.8");
            assertThat(lookupResult.isEmpty()).isFalse();
            assertThat(lookupResult.multiValue()).isNotEmpty();
            assertThat(lookupResult.multiValue())
                    .hasEntrySatisfying("coordinates", value -> assertThat((String) value).matches("[0-9.\\-]+,[0-9.\\-]+"));
        }

        @Test
        public void doGetReturnsResultIfCityResponseFieldsAreNull() throws Exception {
            final CityResponse cityResponse = new CityResponse(null, null, null, null, null, null, null, null, null, null);
            final DatabaseReader mockDatabaseReader = mock(DatabaseReader.class);
            when(mockDatabaseReader.city(any())).thenReturn(cityResponse);
            final DatabaseReader oldDatabaseReader = adapter.getDatabaseReader();

            try {
                adapter.setDatabaseReader(mockDatabaseReader);

                final LookupResult lookupResult = adapter.doGet("127.0.0.1");
                assertThat(lookupResult.isEmpty()).isFalse();
                assertThat(lookupResult.singleValue()).isNull();
            } finally {
                adapter.setDatabaseReader(oldDatabaseReader);
            }
        }
    }

    @RunWith(ConditionalRunner.class)
    @ResourceExistsCondition(GEO_LITE2_COUNTRY_MMDB)
    public static class CountryDatabaseTest extends Base {
        public CountryDatabaseTest() {
            super(DatabaseType.MAXMIND_COUNTRY);
        }

        @Test
        public void doGetSuccessfullyResolvesGooglePublicDNSAddress() {
            // This test will possibly get flaky when the entry for 8.8.8.8 changes!
            final LookupResult lookupResult = adapter.doGet("8.8.8.8");
            assertThat(lookupResult.isEmpty()).isFalse();
            assertThat(lookupResult.multiValue())
                    .extracting("country")
                    .extracting("geoNameId")
                    .containsExactly(6252001);
        }

        @Test
        public void doGetReturnsResultIfCountryResponseFieldsAreNull() throws Exception {
            final CountryResponse countryResponse = new CountryResponse(null, null, null, null, null, null);
            final DatabaseReader mockDatabaseReader = mock(DatabaseReader.class);
            when(mockDatabaseReader.country(any())).thenReturn(countryResponse);
            final DatabaseReader oldDatabaseReader = adapter.getDatabaseReader();

            try {
                adapter.setDatabaseReader(mockDatabaseReader);

                final LookupResult lookupResult = adapter.doGet("127.0.0.1");
                assertThat(lookupResult.isEmpty()).isFalse();
                assertThat(lookupResult.singleValue()).isNull();
            } finally {
                adapter.setDatabaseReader(oldDatabaseReader);
            }
        }
    }
}
