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
package org.graylog.security.certutil.csr.storage;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrFileStorageTest {

    @Test
    void testCsrStorageSaveAndRetrieve(@TempDir Path tmpDir) throws Exception {
        final PKCS10CertificationRequest csr = CsrTestTools.getCsrForTests();
        CsrFileStorage csrFileStorage = new CsrFileStorage(tmpDir.resolve("test.csr").toString());
        csrFileStorage.writeCsr(csr);
        final PKCS10CertificationRequest readCsr = csrFileStorage.readCsr();
        assertEquals(csr, readCsr);
    }


}
