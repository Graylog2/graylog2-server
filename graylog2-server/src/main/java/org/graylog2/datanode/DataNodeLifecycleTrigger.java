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
package org.graylog2.datanode;

public enum DataNodeLifecycleTrigger {
    REMOVE, RESET, STOP, START, REMOVED, STOPPED, STARTED, CLEAR,
    /**
     * Tell the data node that it should request a new certificate, by creating a certificate signing request.
     * This CSR will then be propagated as an event to the certificate authority, that will issue a signed cert
     * and send it as an event back to the data node.
     */
    REQUEST_CERTIFICATE
}
