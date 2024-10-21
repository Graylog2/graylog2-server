package org.graylog.security.certutil;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.time.Duration;

class CertificateGeneratorTest {

    @Test
    void testDomainName() throws Exception {
        final KeyPair pair = selfSigned("www.graylog.org");
        final X509Certificate certificate = pair.certificate();
        final String cn = certificate.getSubjectX500Principal().getName();
        Assertions.assertThat(cn).isEqualTo("CN=www.graylog.org");
    }

    @Test
    void testEscaping() throws Exception {
        final KeyPair pair = selfSigned("Graylog, Inc.");
        final X509Certificate certificate = pair.certificate();
        final String cn = certificate.getSubjectX500Principal().getName();
        Assertions.assertThat(cn).isEqualTo("CN=Graylog\\, Inc.");
    }

    private static KeyPair selfSigned(String cname) throws Exception {
        final CertRequest req = CertRequest.selfSigned(cname).validity(Duration.ofDays(1));
        return CertificateGenerator.generate(req);
    }
}
