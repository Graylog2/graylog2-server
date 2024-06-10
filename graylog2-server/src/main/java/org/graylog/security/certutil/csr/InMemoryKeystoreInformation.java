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

import java.security.KeyStore;

public class InMemoryKeystoreInformation implements KeystoreInformation {

    private final KeyStore keyStore;
    private final char[] password;

    public InMemoryKeystoreInformation(KeyStore keyStore, char[] password) {
        this.keyStore = keyStore;
        this.password = password;
    }

    @Override
    public KeyStore loadKeystore() throws Exception {
        return keyStore;
    }

    @Override
    public char[] password() {
        return password;
    }
}
