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
package integration;

import com.github.joschi.nosqlunit.elasticsearch2.ElasticsearchRule;
import org.elasticsearch.common.settings.Settings;
import org.junit.Rule;

import static com.github.joschi.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.github.joschi.nosqlunit.elasticsearch2.RemoteElasticsearchConfigurationBuilder.remoteElasticsearch;


public class RestTestIncludingElasticsearch extends BaseRestTest {
    @Rule
    public ElasticsearchRule elasticsearchRule = newElasticsearchRule().configure(remoteElasticsearch()
            .host(IntegrationTestsConfig.getEsHost())
            .settings(Settings.builder().put("cluster.name", IntegrationTestsConfig.getEsClusterName()).build())
            .port(IntegrationTestsConfig.getEsPort()).build()).build();

}
