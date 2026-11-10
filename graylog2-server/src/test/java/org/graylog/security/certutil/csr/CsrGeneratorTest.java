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
package org.graylog.security.certutil.csr;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsrGeneratorTest {

    private static final CertificateGenerator CERTIFICATE_GENERATOR = new CertificateGenerator(1024);

    @Test
    void encodesIpLiteralAltNamesAsIPAddressSAN() throws Exception {
        final String keyAlias = "datanode";
        final char[] password = "password".toCharArray();
        final KeyPair keyPair = CERTIFICATE_GENERATOR.generateKeyPair(CertRequest.selfSigned(keyAlias).isCA(false).validity(Duration.ofDays(30)));
        final InMemoryKeystoreInformation keystoreInformation = new InMemoryKeystoreInformation(keyPair.toKeystore(keyAlias, password), password);

        final PKCS10CertificationRequest csr = CsrGenerator.generateCSR(keystoreInformation, keyAlias, "datanode.example.org",
                List.of("10.100.100.93", "another-datanode-hostname"));

        final GeneralName[] generalNames = extractSubjectAlternativeNames(csr);

        final List<String> ipAddressNames = Arrays.stream(generalNames)
                .filter(name -> name.getTagNo() == GeneralName.iPAddress)
                .map(this::decodeIpAddress)
                .toList();
        final List<String> dnsNames = Arrays.stream(generalNames)
                .filter(name -> name.getTagNo() == GeneralName.dNSName)
                .map(name -> name.getName().toString())
                .toList();

        assertThat(ipAddressNames).containsExactly("10.100.100.93");
        assertThat(dnsNames).containsExactlyInAnyOrder("datanode.example.org", "another-datanode-hostname");
    }

    private String decodeIpAddress(GeneralName name) throws RuntimeException {
        try {
            final byte[] octets = ASN1OctetString.getInstance(name.getName()).getOctets();
            return InetAddress.getByAddress(octets).getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GeneralName[] extractSubjectAlternativeNames(PKCS10CertificationRequest csr) {
        final var attribute = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)[0];
        final Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
        final GeneralNames generalNames = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);

        return generalNames.getNames();
    }
}
