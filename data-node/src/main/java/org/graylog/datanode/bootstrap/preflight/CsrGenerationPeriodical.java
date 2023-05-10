package org.graylog.datanode.bootstrap.preflight;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog2.cluster.NodePreflightConfig;
import org.graylog2.cluster.NodePreflightConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

@Singleton
public class CsrGenerationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(CsrGenerationPeriodical.class);

    private final NodePreflightConfigService nodePreflightConfigService;
    private final NodeId nodeId;
    private final PrivateKeyEncryptedFileStorage privateKeyEncryptedStorage;

    @Inject
    public CsrGenerationPeriodical(final NodePreflightConfigService nodePreflightConfigService, final NodeId nodeId) {
        this.nodePreflightConfigService = nodePreflightConfigService;
        this.nodeId = nodeId;
        this.privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage("privateKeyFilename.cert");
    }

    @Override
    public void doRun() {
        LOG.info("checking if I'm supposed to create a CSR");
        var cfg = nodePreflightConfigService.getPreflightConfigFor(nodeId.getNodeId());
        if (cfg != null && NodePreflightConfig.State.CONFIGURED.equals(cfg.state())) {
            try {
                var csr = new CsrGenerator().generateCSR("admin".toCharArray(), "principal", cfg.altNames(), privateKeyEncryptedStorage);
                var sw = new StringWriter();
                var jcaPEMWriter = new JcaPEMWriter(sw);
                jcaPEMWriter.writeObject(csr);
                jcaPEMWriter.flush();
                nodePreflightConfigService.save(cfg.toBuilder().state(NodePreflightConfig.State.CSR).csr(sw.toString()).build());
                LOG.info("created CSR for this node");
            } catch (CSRGenerationException | IOException ex) {
                LOG.error("Error generating a CSR: " + ex.getMessage(), ex);
                nodePreflightConfigService.save(cfg.toBuilder().state(NodePreflightConfig.State.ERROR).errorMsg(ex.getMessage()).build());
            }
        }
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
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 2;
    }
}
