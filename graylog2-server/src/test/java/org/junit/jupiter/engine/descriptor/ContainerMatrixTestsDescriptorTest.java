package org.junit.jupiter.engine.descriptor;

import com.github.zafarkhaja.semver.Version;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerMatrixTestsDescriptorTest {

    @Test
    void createKey() {
        final String key = ContainerMatrixTestsDescriptor.createKey(Lifecycle.CLASS, "mavenDir", "jarDir", SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.2.3")), MongodbServer.MONGO4, true);
        Assertions.assertThat(key).isEqualTo("Lifecycle: CLASS, MavenProjectDirProvider: mavenDir, PluginJarsProvider: jarDir, Search: OpenSearch:1.2.3, MongoDB: 4.0, Mailserver: enabled");
    }
}
