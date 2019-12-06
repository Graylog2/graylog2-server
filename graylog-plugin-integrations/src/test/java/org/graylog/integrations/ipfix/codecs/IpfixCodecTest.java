package org.graylog.integrations.ipfix.codecs;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Ignore("All tests in this class are in development and not yet ready.")
public class IpfixCodecTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private IpfixCodec codec;
    private IpfixAggregator ipfixAggregator;

    @Before
    public void setUp() throws Exception {
        ipfixAggregator = new IpfixAggregator();
        codec = new IpfixCodec(Configuration.EMPTY_CONFIGURATION, ipfixAggregator);
    }

    @Ignore("Invalid CK_IPFIX_DEFINITION_PATH does not throw IOException, feature not ready.")
    @Test
    public void constructorFailsIfIPFixDefinitionsPathDoesNotExist() throws Exception {
        final File definitionsFile = temporaryFolder.newFile();
        assertThat(definitionsFile.delete()).isTrue();
        final ImmutableMap<String, Object> configMap = ImmutableMap.of(
                IpfixCodec.CK_IPFIX_DEFINITION_PATH, definitionsFile.getAbsolutePath());
        final Configuration configuration = new Configuration(configMap);
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> new IpfixCodec(configuration, ipfixAggregator))
                .withMessageEndingWith("(No such file or directory)");
    }

}