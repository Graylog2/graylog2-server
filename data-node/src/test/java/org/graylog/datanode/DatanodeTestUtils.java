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
package org.graylog.datanode;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class DatanodeTestUtils {
    public static Configuration datanodeConfiguration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final Configuration configuration = new Configuration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "password_secret", "thisisverysecretpassword"
        ));
        new JadConfig(List.of(mandatoryProps, new InMemoryRepository(properties)), configuration).process();
        return configuration;
    }

    public static DatanodeDirectories tempDirectories(Path tempDir) {
        return new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir);
    }

    public static KeyPair generateKeyPair(Duration validity) throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(RandomStringUtils.randomAlphabetic(10))
                .isCA(false)
                .validity(validity);
        return CertificateGenerator.generate(certRequest);
    }
}
