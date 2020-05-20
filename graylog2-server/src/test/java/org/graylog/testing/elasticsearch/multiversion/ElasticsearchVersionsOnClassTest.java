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
