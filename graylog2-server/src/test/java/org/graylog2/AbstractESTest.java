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
package org.graylog2;

import com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import javax.inject.Inject;

import static com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;

public abstract class AbstractESTest {
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule().build();

    @Rule
    public ElasticsearchRule elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();

    @Inject
    protected Client client;

    @Before
    public void setUp() throws Exception {
        elasticsearchRule.getDatabaseOperation().deleteAll();
    }
}
