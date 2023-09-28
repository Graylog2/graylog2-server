package org.graylog.security.certutil.csr;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class CsrSignerTest {
    private static final X500Name subjectName = new X500Name("CN=Example Request");

    @BeforeEach
    void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void testSigningCertWithSixMonthsLifetime() throws Exception {
        var keyPair = createPrivateKey();
        var cert = createCert(keyPair);
        var privateKey = keyPair.getPrivate();
        var csr = createCSR(keyPair);
        assertThat(new CsrSigner().sign(privateKey, cert, csr, new RenewalPolicy(RenewalPolicy.Mode.AUTOMATIC, "P6M"))).isNotNull();
    }

    private PKCS10CertificationRequest createCSR(KeyPair keyPair) throws OperatorCreationException {
        var contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        var pkcs10Builder = new PKCS10CertificationRequestBuilder(subjectName, SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        return pkcs10Builder.build(contentSigner);
    }

    private X509Certificate createCert(KeyPair keyPair) throws OperatorCreationException, CertificateException {
        var startDate = Date.from(Instant.now());
        var endDate = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        var serialNumber = new BigInteger(128, new SecureRandom());

        var certBuilder = new X509v3CertificateBuilder(
                subjectName,
                serialNumber,
                startDate,
                endDate,
                subjectName,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        var privateKey = keyPair.getPrivate();
        var certHolder = certBuilder.build(
                new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256WithRSA")
                        .setProvider("BC")
                        .build(privateKey)
        );

        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    private KeyPair createPrivateKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }
}
