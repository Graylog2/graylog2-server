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

import java.util.Collection;
import java.util.List;

public interface KeystoreMongoCollections {

    KeystoreMongoCollection DATA_NODE_KEYSTORE_COLLECTION = new KeystoreMongoCollection(
            "data_node_certificates",
            "node_id",
            "encrypted_certificate_keystore"
    );

    Collection<KeystoreMongoCollection> ALL_KEYSTORE_COLLECTIONS = List.of(
            DATA_NODE_KEYSTORE_COLLECTION
    );

}
