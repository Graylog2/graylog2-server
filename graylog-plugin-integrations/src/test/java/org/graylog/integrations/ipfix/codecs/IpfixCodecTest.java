package org.graylog.integrations.ipfix.codecs;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog.integrations.ipfix.InformationElementDefinitions;
import org.graylog2.plugin.configuration.Configuration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertNotNull;

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

    private URL convertToURL(@NotNull String s) throws MalformedURLException {
        return Paths.get(s).toUri().toURL();
    }


    @Test
    public void buildIPFixWithStandardAndCustomDefinition() throws MalformedURLException, Exception {
        // Assume these comma seperated files exist in the path
        String expectedValue = "/home/helium/workspace/config/test.json,/home/helium/workspace/config/test1.json";
        List<String> urls = Arrays.asList(expectedValue.split(","));
        assertThat(urls.size()).isEqualTo(2);

        // Add the standard definition files
        List<URL> urlList = new ArrayList<>();
        final URL standardIPFixDefTemplate = Resources.getResource(IpfixCodec.class, IpfixCodec.IPFIX_STANDARD_DEFINITION);
        urlList.add(standardIPFixDefTemplate);

        //Add the Optional definition files
        for (String url : urls) {
            URL convertToURL = convertToURL(url);
            urlList.add(convertToURL);
        }
        // Validate URLList has 3 elements
        urlList.stream().forEach(System.out::println);
        assertThat(urlList.toArray().length).isEqualTo(3);

        URL[] urlArray = new URL[urlList.size()];
        urlArray = urlList.toArray(urlArray);
        // construct ied with 3 files. One standard and 2 Optional
        InformationElementDefinitions ieds = new InformationElementDefinitions(urlArray);
        assertNotNull(ieds);

    }


}