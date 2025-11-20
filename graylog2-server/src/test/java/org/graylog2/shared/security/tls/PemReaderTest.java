/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.security.tls;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyException;
import java.security.cert.CertificateException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PemReaderTest {
    @TempDir
    public File temporaryFolder;

    @Test
    public void readCertificatesHandlesSingleCertificate() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/single.crt");
        final List<byte[]> certificates = PemReader.readCertificates(Paths.get(url.toURI()));
        assertThat(certificates).hasSize(1);
    }

    @Test
    public void readCertificatesHandlesCertificateChain() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/chain.crt");
        final List<byte[]> certificates = PemReader.readCertificates(Paths.get(url.toURI()));
        assertThat(certificates).hasSize(2);
    }

    @Test
    public void readCertificatesFailsOnInvalidFile() throws Exception {
        assertThrows(CertificateException.class, () -> {
            final File file = File.createTempFile("junit", null, temporaryFolder);
            PemReader.readCertificates(file.toPath());
        });
    }

    @Test
    public void readCertificatesFailsOnDirectory() throws Exception {
        assertThrows(CertificateException.class, () -> {
            final File folder = newFolder(temporaryFolder, "junit");
            PemReader.readCertificates(folder.toPath());
        });
    }

    @Test
    public void readPrivateKeyHandlesPrivateKey() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/private.key");
        final byte[] privateKey = PemReader.readPrivateKey(Paths.get(url.toURI()));
        assertThat(privateKey).isNotEmpty();
    }

    @Test
    public void readPrivateKeyHandlesSecuredPrivateKey() throws Exception {
        final URL url = Resources.getResource("org/graylog2/shared/security/tls/key-enc-pbe1.p8");
        final byte[] privateKey = PemReader.readPrivateKey(Paths.get(url.toURI()));
        assertThat(privateKey).isNotEmpty();
    }

    @Test
    public void readPrivateKeyFailsOnInvalidFile() throws Exception {
        assertThrows(KeyException.class, () -> {
            final File file = File.createTempFile("junit", null, temporaryFolder);
            PemReader.readPrivateKey(file.toPath());
        });
    }

    @Test
    public void readPrivateKeyFailsOnDirectory() throws Exception {
        assertThrows(KeyException.class, () -> {
            final File folder = newFolder(temporaryFolder, "junit");
            PemReader.readPrivateKey(folder.toPath());
        });
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
