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
package org.graylog.testing.ldap;

import com.google.common.io.Resources;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;

public class LDAPTestUtils {
    private static final String RESOURCE_ROOT = "org/graylog/testing/ldap";

    public static final String BASE_LDIF = RESOURCE_ROOT + "/ldif/base.ldif";
    public static final String NESTED_GROUPS_LDIF = RESOURCE_ROOT + "/ldif/nested-groups.ldif";

    public static String testTLSCertsPath() {
        final URL resourceUrl = Resources.getResource(RESOURCE_ROOT + "/certs");
        try {
            return Paths.get(resourceUrl.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyStore getKeystore(String filename) {
        try {
            final KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Resources.getResource(RESOURCE_ROOT + "/" + filename).openStream(), "changeit".toCharArray());
            return keystore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getResourcePath(String resourcePath) {
        final URL resourceUrl = Resources.getResource(resourcePath);
        try {
            return Paths.get(resourceUrl.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
