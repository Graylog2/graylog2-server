package org.graylog2.plugin.inputs.transports.util;

import com.google.common.io.Resources;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyUtilNonParameterizedTest {
    @BeforeAll
    public void init() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return new File(Resources.getResource("org/graylog2/plugin/inputs/transports/util/" + fileName).toURI());
    }

    @Test
    public void testPrivateKeyFromProtectedFile() throws URISyntaxException, IOException, OperatorCreationException, PKCSException {
        final File keyFile = resourceToFile("server.key.pem.pkcs8.protected");
        final PrivateKey privateKey = KeyUtil.privateKeyFromFile("test", keyFile);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testPrivateKeyFromUnprotectedFile() throws URISyntaxException, IOException, OperatorCreationException, PKCSException {
        final File keyFile = resourceToFile("server.key.pem.pkcs8.unprotected");
        final PrivateKey privateKey = KeyUtil.privateKeyFromFile("", keyFile);
        assertThat(privateKey).isNotNull();
    }
}
