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
package org.graylog.datanode.bootstrap.preflight;

import jakarta.inject.Inject;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatanodeCertReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeCertReceiver.class);

    private final NodeId nodeId;

    private final CertificateExchange certificateExchange;
    private final DatanodeKeystore datanodeKeystore;

    @Inject
    public DatanodeCertReceiver(NodeId nodeId, CertificateExchange certificateExchange, DatanodeKeystore datanodeKeystore) {
        this.nodeId = nodeId;
        this.certificateExchange = certificateExchange;
        this.datanodeKeystore = datanodeKeystore;
    }

    public void pollCertificate() {
        // always check if there are any certificates that we can accept
        certificateExchange.pollCertificate(nodeId.getNodeId(), this::processCertificateChain);
    }

    private void processCertificateChain(CertificateChain certificateChain) {
        try {
            LOG.info("Received new datanode certificate, updating datanode keystore");
            datanodeKeystore.replaceCertificatesInKeystore(certificateChain);
        } catch (DatanodeKeystoreException e) {
            throw new RuntimeException(e);
        }
    }
}
