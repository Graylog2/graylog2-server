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
package org.graylog2.bundles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.BundleExporterProvider;
import org.graylog2.bindings.providers.BundleImporterProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class BundleServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BundleImporterProvider bundleImporterProvider;
    @Mock
    private BundleExporterProvider bundleExporterProvider;

    private BundleService bundleService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        bundleService = new BundleService(
            new MongoJackObjectMapperProvider(objectMapper),
            mongoRule.getMongoConnection(),
            bundleImporterProvider,
            bundleExporterProvider);
    }

    @Test
    public void updateSucceedsInUpdatingExistingBundle() throws Exception {
        final ConfigurationBundle bundle = bundleService.insert(new ConfigurationBundle());
        final ConfigurationBundle newBundle = new ConfigurationBundle();
        newBundle.setName("Test");
        assertThat(bundleService.update(bundle.getId(), newBundle)).isTrue();
        final ConfigurationBundle updatedBundle = bundleService.load(bundle.getId());
        assertThat(updatedBundle.getId()).isEqualTo(bundle.getId());
        assertThat(updatedBundle.getName())
            .isNotEqualTo(bundle.getName())
            .isEqualTo(newBundle.getName());
    }

    @Test
    public void updateFailsInUpdatingNonExistingBundle() throws Exception {
        final ConfigurationBundle bundle = new ConfigurationBundle();
        bundle.setName("Test");
        assertThat(bundleService.update("571f1b5d35f5441210c5434e", bundle)).isFalse();
    }
}
