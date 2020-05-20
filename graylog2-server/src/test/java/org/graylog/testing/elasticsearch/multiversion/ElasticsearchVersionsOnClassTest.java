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
package org.graylog.testing.elasticsearch.multiversion;

import org.graylog.testing.elasticsearch.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.elasticsearch.multiversion.FetchVersionUtil.fetchVersion;

@ElasticsearchVersions
public class ElasticsearchVersionsOnClassTest {

    private Client clientFromSetup;

    @BeforeEach
    void setUp(Client client) {
        clientFromSetup = client;
    }

    @TestTemplate
    void clientWasInjectedInSetup() {
        assertThat(clientFromSetup).as("client should be injectable in setup methods").isNotNull();
    }

    @TestTemplate
    void worksWithAnnotationOnClass(Client client, String version) {
        String actualVersion = fetchVersion(client);

        assertThat(actualVersion).isEqualTo(version);
    }
}
