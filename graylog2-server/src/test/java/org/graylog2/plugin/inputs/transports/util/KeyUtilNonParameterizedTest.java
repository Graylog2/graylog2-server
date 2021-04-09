package org.graylog2.plugin.inputs.transports.util;

import com.google.common.io.Resources;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyUtilNonParameterizedTest {
    @Before
    public void init() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Test
    public void testPrivateKeyFromProtectedFile() throws URISyntaxException, IOException, OperatorCreationException, PKCSException {
        final String password = "test";
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.protected"), password);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testPrivateKeyFromUnprotectedFile() throws URISyntaxException, IOException, OperatorCreationException, PKCSException {
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.unprotected"), null);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testGeneratePKSC8PrivateKey() throws GeneralSecurityException, IOException, OperatorCreationException, PKCSException, URISyntaxException {
        final PrivateKey privateKey = fileToKey(resourceToFile("server.key.pem.pkcs8.unprotected"), null);
        final String tmpPassword = "dummypassword";
        File pkcs8EncryptedKeyFile = KeyUtil.generatePKSC8PrivateKey(tmpPassword.toCharArray(), privateKey);
        final PrivateKey retrievedKey = fileToKey(pkcs8EncryptedKeyFile, tmpPassword);
        assertThat(retrievedKey.equals(privateKey));
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return new File(Resources.getResource("org/graylog2/plugin/inputs/transports/util/" + fileName).toURI());
    }

    private PrivateKey fileToKey(File keyFile, String password) throws URISyntaxException, IOException, OperatorCreationException, PKCSException {
        return KeyUtil.privateKeyFromFile(password, keyFile);
    }
}
