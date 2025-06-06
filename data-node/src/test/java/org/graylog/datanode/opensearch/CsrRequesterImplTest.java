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
package org.graylog.datanode.opensearch;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.google.common.eventbus.EventBus;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.graylog.security.certutil.cert.CertificateChain;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateSigningRequest;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

class CsrRequesterImplTest {

    @Test
    void testSAN(@TempDir Path tempDir) throws Exception {
        final Configuration configuration = DatanodeTestUtils.datanodeConfiguration(Map.of(
                "node_name", "my-node-name",
                "hostname", "my-datanode-machine"
        ));

        final DatanodeKeystore datanodeKeystore = new DatanodeKeystore(new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir), "foobar", new EventBus());
        datanodeKeystore.create(generateKeyPair(Duration.ofDays(30)));


        Queue<CertificateSigningRequest> signingRequests = new LinkedList<>();
        final CsrRequester requester = new CsrRequesterImpl(configuration, new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"), datanodeKeystore, mockCertificateExchange(signingRequests));

        requester.triggerCertificateSigningRequest();

        Assertions.assertThat(signingRequests.poll())
                .isNotNull()
                .satisfies(signingRequest -> {
                    final PKCS10CertificationRequest req = signingRequest.request();
                    final List<String> names = getSubjectAlternativeNames(req);
                    Assertions.assertThat(names)
                            .isNotNull()
                            .contains("my-node-name", "my-datanode-machine");
                });
    }

    public static List<String> getSubjectAlternativeNames(PKCS10CertificationRequest csr) throws IOException {
        List<String> sanList = new ArrayList<>();

        // Find the extension request attribute
        Attribute[] attributes = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        if (attributes == null || attributes.length == 0) {
            return sanList; // No extensions present
        }

        Extensions extensions = Extensions.getInstance(attributes[0].getAttrValues().getObjectAt(0));
        GeneralNames sans = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);

        if (sans != null) {
            for (GeneralName name : sans.getNames()) {
                switch (name.getTagNo()) {
                    case GeneralName.dNSName:
                    case GeneralName.rfc822Name:
                    case GeneralName.iPAddress:
                        sanList.add(name.getName().toString());
                        break;
                    // You can handle other tag types as needed
                }
            }
        }

        return sanList;
    }

    private KeyPair generateKeyPair(Duration duration) throws Exception {
        final CertRequest certRequest = CertRequest.selfSigned(DatanodeKeystore.DATANODE_KEY_ALIAS)
                .isCA(false)
                .validity(duration);
        return CertificateGenerator.generate(certRequest);
    }

    @Nonnull
    private static CertificateExchange mockCertificateExchange(Queue<CertificateSigningRequest> queue) {
        return new CertificateExchange() {
            @Override
            public void requestCertificate(CertificateSigningRequest request) throws IOException {
                queue.add(request);
            }

            @Override
            public void signPendingCertificateRequests(Function<CertificateSigningRequest, CertificateChain> signingFunction) throws IOException {

            }

            @Override
            public void pollCertificate(String nodeId, Consumer<CertificateChain> chainConsumer) {

            }
        };
    }
}
