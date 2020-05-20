package org.graylog.testing.elasticsearch.multiversion;

import org.graylog.testing.elasticsearch.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchVersionOnMethodTest {

    private Client clientFromSetup;

    @BeforeEach
    void setUp(Client client) {
        clientFromSetup = client;
    }

    @Disabled(value = "This test illustrates that injection in the setup method will fail, "
            + "if ElasticsearchVersions isn't specified on ALL test methods.")
    @Test
    void thisTestWouldFail() {
        assertThat(1).isEqualTo(1);
    }

    @ElasticsearchVersions
    @TestTemplate
    void clientWasInjectedInSetup() {
        assertThat(clientFromSetup).as("client should be injectable in setup methods").isNotNull();
    }

    @ElasticsearchVersions(versions = {"6.8.4"})
    @TestTemplate
    void annotationOnMethodWorks(String version) {
        assertThat(version).isEqualTo("6.8.4");
    }
}
