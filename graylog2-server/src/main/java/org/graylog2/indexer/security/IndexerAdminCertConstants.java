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
package org.graylog2.indexer.security;

/**
 * Shared identity used for short-lived OpenSearch admin certificates that bypass the
 * security plugin (see {@code plugins.security.authcz.admin_dn} on the Data Node side).
 * Both the certificate-minting code on the server and the Data Node's OpenSearch
 * security config must agree on this value.
 */
public final class IndexerAdminCertConstants {
    public static final String ADMIN_CN = "graylog-admin";
    public static final String ADMIN_DN = "CN=" + ADMIN_CN;

    private IndexerAdminCertConstants() {}
}
