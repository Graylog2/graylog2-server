package org.graylog.security.certutil.cert.storage;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;

public class CertMongoStorage implements CertStorage {

    private NodePreflightConfigService mongoService;
    private NodeId nodeId;

    @Inject
    public CertMongoStorage(final NodePreflightConfigService mongoService,
                           final NodeId nodeId) {
        this.mongoService = mongoService;
        this.nodeId = nodeId;
    }

    public void writeCert(X509Certificate cert) throws IOException, OperatorCreationException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(cert);
        }
        mongoService.writeCert(nodeId.getNodeId(), writer.toString());
    }
}
