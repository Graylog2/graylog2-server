package org.graylog.security.certutil.cert.storage;

import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.cert.X509Certificate;

public interface CertStorage {
    void writeCert(X509Certificate cert)
            throws IOException, OperatorCreationException;
}
