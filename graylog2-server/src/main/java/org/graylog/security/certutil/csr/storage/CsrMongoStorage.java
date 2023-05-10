package org.graylog.security.certutil.csr.storage;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog2.cluster.NodePreflightConfigService;

import javax.inject.Inject;
import java.io.IOException;

public class CsrMongoStorage implements CsrStorage {
    private final NodePreflightConfigService nodePreflightConfigService;

    @Inject
    public CsrMongoStorage(final NodePreflightConfigService nodePreflightConfigService) {
        this.nodePreflightConfigService = nodePreflightConfigService;
    }

    @Override
    public void writeCsr(PKCS10CertificationRequest csr) throws IOException, OperatorCreationException {

    }

    @Override
    public PKCS10CertificationRequest readCsr() throws IOException, OperatorCreationException {
        return null;
    }
}
