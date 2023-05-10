package org.graylog2.bootstrap.preflight;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Singleton
public class CsrSigningPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(CsrSigningPeriodical.class);

    private final NodePreflightConfigService nodePreflightConfigService;

    @Inject
    public CsrSigningPeriodical(final NodePreflightConfigService nodePreflightConfigService) {
        this.nodePreflightConfigService = nodePreflightConfigService;
        loadCA();
    }

    private String caKeystoreFilename = "datanode-ca.p12";
    private PrivateKey caPrivateKey;
    private X509Certificate caCertificate;

    private void loadCA() {
        final Path caKeystorePath = Path.of(caKeystoreFilename);

        try {
            char[] password = "".toCharArray();
            KeyStore caKeystore = KeyStore.getInstance("PKCS12");
            caKeystore.load(new FileInputStream(caKeystorePath.toFile()), password);

            caPrivateKey = (PrivateKey) caKeystore.getKey("ca", password);
            caCertificate = (X509Certificate) caKeystore.getCertificate("ca");
            final X509Certificate rootCertificate = (X509Certificate) caKeystore.getCertificate("root");
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException |
                 UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PKCS10CertificationRequest readCsr(String pem)
            throws IOException {

        Reader pemReader = new BufferedReader(new StringReader(pem));
        PEMParser pemParser = new PEMParser(pemReader);
        Object parsedObj = pemParser.readObject();
        if (parsedObj instanceof PKCS10CertificationRequest) {
            return (PKCS10CertificationRequest) parsedObj;
        }
       throw new IOException("Could not decode PEM");
    }

    private String writeCert(X509Certificate cert)
            throws IOException {

        var sw = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(sw)) {
            jcaPEMWriter.writeObject(cert);
        }
        return sw.toString();
    }

    @Override
    public void doRun() {
        LOG.info("checking for CSRs to sign...");
        // if ca exists
        nodePreflightConfigService.streamAll()
                .filter(c -> NodePreflightConfig.State.CSR.equals(c.state()))
                .map(c -> {
                    try {
                        var csr = readCsr(c.csr());
                        var cert = CsrSigner.sign(caPrivateKey, caCertificate, csr, 90);
                        return c.toBuilder().certificate(this.writeCert(cert)).state(NodePreflightConfig.State.SIGNED).build();
                    } catch (Exception e) {
                        LOG.error("Could not sign CSR: " + e.getMessage(), e);
                        return c.toBuilder().state(NodePreflightConfig.State.ERROR).errorMsg(e.getMessage()).build();
                    }
                 })
                .forEach(nodePreflightConfigService::save);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 2;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
