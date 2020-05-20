package org.graylog.testing.elasticsearch.multiversion;

import org.graylog.testing.elasticsearch.Client;
import org.junit.jupiter.api.TestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.elasticsearch.multiversion.FetchVersionUtil.fetchVersion;

@ElasticsearchVersions
public class ElasticsearchVersionsOnMethodAndClassTest {
    @TestTemplate
    void worksWithAnnotationOnClass(Client client, String version) {
        String actualVersion = fetchVersion(client);

        assertThat(actualVersion).isEqualTo(version);
    }

    @ElasticsearchVersions(versions = {"6.8.3"})
    @TestTemplate
    void annotationOnMethodTakesPrecedence(String version) {
        assertThat(version).isEqualTo("6.8.3");
    }
}
