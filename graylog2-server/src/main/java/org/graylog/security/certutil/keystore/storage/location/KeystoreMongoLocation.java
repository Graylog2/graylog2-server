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
package org.graylog.security.certutil.keystore.storage.location;

import org.graylog2.plugin.system.NodeId;

public class KeystoreMongoLocation implements KeystoreLocation {
    private final String nodeId;
    private final KeystoreMongoCollection collection;

    public static final String CA_KEYSTORE_ID = "GRAYLOG CA";

    private KeystoreMongoLocation(String nodeId, KeystoreMongoCollection collection) {
        this.nodeId = nodeId;
        this.collection = collection;
    }


    public static KeystoreMongoLocation certificateAuthority() {
        return new KeystoreMongoLocation(CA_KEYSTORE_ID, KeystoreMongoCollections.GRAYLOG_CA_KEYSTORE_COLLECTION);
    }

    public static KeystoreMongoLocation datanode(NodeId nodeId) {
        return datanode(nodeId.getNodeId());
    }

    public static KeystoreMongoLocation datanode(String nodeId) {
        return new KeystoreMongoLocation(nodeId, KeystoreMongoCollections.DATA_NODE_KEYSTORE_COLLECTION);
    }


    public String nodeId() {
        return nodeId;
    }

    public KeystoreMongoCollection collection() {
        return collection;
    }
}
