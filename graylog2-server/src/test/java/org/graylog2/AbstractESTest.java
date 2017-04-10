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
