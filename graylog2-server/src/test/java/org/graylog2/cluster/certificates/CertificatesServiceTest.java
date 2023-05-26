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
package org.graylog2.cluster.certificates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueDeserializer;
import org.graylog2.security.encryption.EncryptedValueSerializer;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CertificatesServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private CertificatesService certificatesService;
    private EncryptedValueService encryptedValueService;

    @Before
    public void setUp() {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        encryptedValueService = new EncryptedValueService("abracadabra! abracadabra!");
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(
                new ObjectMapper().registerModule(new SimpleModule("Graylog")
                        .addSerializer(EncryptedValue.class, new EncryptedValueSerializer())
                        .addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer(encryptedValueService))
                )

        );
        certificatesService = new CertificatesService(objectMapperProvider, mongoConnection, encryptedValueService);

    }

    @Test
    public void testReadUnexisting() {
        final Optional<String> result = certificatesService.readCert("there is no node with this id");
        assertTrue(result.isEmpty());

    }

    @Test
    public void testReadAndWrite() {
        boolean writeResult = certificatesService.writeCert("node_id_1", "Certificate string representation");
        assertTrue(writeResult);
        final Optional<String> readResult = certificatesService.readCert("node_id_1");
        assertTrue(readResult.isPresent());
        assertEquals("Certificate string representation", readResult.get());
    }


}
