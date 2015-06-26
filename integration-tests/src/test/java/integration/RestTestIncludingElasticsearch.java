package integration;

import com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.Rule;

import static com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch.RemoteElasticsearchConfigurationBuilder.remoteElasticsearch;

public class RestTestIncludingElasticsearch extends BaseRestTest {
    @Rule
    public ElasticsearchRule elasticsearchRule = newElasticsearchRule().configure(remoteElasticsearch()
            .host(IntegrationTestsConfig.getEsHost())
            .settings(ImmutableSettings.builder().put("cluster.name", IntegrationTestsConfig.getEsClusterName()).build())
            .port(IntegrationTestsConfig.getEsPort()).build()).build();

}
