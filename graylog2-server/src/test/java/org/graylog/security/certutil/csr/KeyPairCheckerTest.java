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
package org.graylog.security.certutil.csr;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyPairCheckerTest {

    @Test
    void testTrueOnValidKeyPair() throws Exception {
        KeyPairChecker toTest = new KeyPairChecker();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen.initialize(2048);
        final KeyPair keyPair = keyGen.genKeyPair();
        assertTrue(toTest.matchingKeys(keyPair.getPrivate(), keyPair.getPublic()));
    }

    @Test
    void testFalseOnInValidKeyPair() throws Exception {
        KeyPairChecker toTest = new KeyPairChecker();
        KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen1.initialize(2048);
        final KeyPair keyPair1 = keyGen1.genKeyPair();
        KeyPairGenerator keyGen2 = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        keyGen2.initialize(2048);
        final KeyPair keyPair2 = keyGen2.genKeyPair();
        //mixing keys from different pairs
        assertFalse(toTest.matchingKeys(keyPair1.getPrivate(), keyPair2.getPublic()));
    }


}
