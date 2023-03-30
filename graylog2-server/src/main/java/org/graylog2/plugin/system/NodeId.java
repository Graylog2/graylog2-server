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
package org.graylog2.plugin.system;

import com.google.common.hash.Hashing;
import org.graylog2.plugin.Tools;

import java.nio.charset.StandardCharsets;

/**
 * This should be an interface. But we need to persist the backwards compatibility with all 5.x releases and keep it a class
 * Please change to an interface for the 6.0 release.
 */
public abstract class NodeId {

    /**
     * @return The server expects UUID style of node id.
     * @see Tools#generateServerId()
     */
    public abstract String getNodeId();


    public String toEscapedString() {
        return getNodeId().replace("\\", "\\\\").replace("$", "\\u0024").replace(".", "\\u002e");
    }

    /**
     * Is it used somewhere in integrations? Should we remove it in 6.0?
     */
    @Deprecated
    public String toUnescapedString() {
        return getNodeId().replace("\\u002e", ".").replace("\\u0024", "$").replace("\\\\", "\\");
    }

    /**
     * Generate an "anonymized" node ID for use with external services. Currently it just hashes the actual node ID
     * using SHA-256.
     *
     * @return The anonymized ID derived from hashing the node ID.
     */
    public String anonymize() {
        return Hashing.sha256().hashString(getNodeId(), StandardCharsets.UTF_8).toString();
    }

    public String getShortNodeId() {
        return getNodeId().split("-")[0];
    }
}
