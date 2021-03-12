package org.graylog2.configuration;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.graylog2.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathConfigurationTest {
    public static final String BIN_PATH = "bin";
    public static final String DATA_PATH = "data";
    public static final String PLUGINS_PATH = "PLUGINS";
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Map<String, String> validProperties;

    @Before
    public void setUp() throws Exception {
        validProperties = new HashMap<>();

        // Required properties
        validProperties.put("bin_dir", BIN_PATH);
        validProperties.put("data_dir", DATA_PATH);
        validProperties.put("plugin_dir", PLUGINS_PATH);
    }

    @Test
    public void testBaseConfiguration() throws ValidationException, RepositoryException {
        PathConfiguration configuration = new PathConfiguration();
        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(validProperties), configuration);
        jadConfig.process();
        assertEquals(BIN_PATH, configuration.getBinDir().toString());
        assertEquals(DATA_PATH, configuration.getDataDir().toString());
        assertEquals(PLUGINS_PATH, configuration.getPluginDir().toString());
        assertTrue(configuration.getTrustedFilePaths().isEmpty());
    }

    @Test
    public void testTrustedPaths() throws ValidationException, RepositoryException {
        validProperties.put("trusted_data_file_paths", "/permitted-dir,/another-valid-dir");
        PathConfiguration configuration = new PathConfiguration();
        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(validProperties), configuration);
        jadConfig.process();
        assertEquals(2,configuration.getTrustedFilePaths().size());
    }
}
