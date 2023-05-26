package org.graylog.datanode.configuration.certificates;

import org.graylog.security.certutil.CertConstants;

public record CertificateMetaData(String alias,
                                  String keystoreFilePath,
                                  char[] keystoreFilePassword) {

    public CertificateMetaData(String keystoreFilePath,
                               char[] keystoreFilePassword) {
        this(CertConstants.DATANODE_KEY_ALIAS, keystoreFilePath, keystoreFilePassword);
    }

    public String passwordAsString() {
        return new String(keystoreFilePassword());
    }

}
