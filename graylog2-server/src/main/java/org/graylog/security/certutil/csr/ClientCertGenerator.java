package org.graylog.security.certutil.csr;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class ClientCertGenerator {
    public PKCS10CertificationRequest generateCSR(final char[] privateKeyPassword,
                                                  final String principalName,
                                                  final List<String> altNames,
                                                  final PrivateKeyEncryptedStorage privateKeyEncryptedStorage) throws CSRGenerationException {
        try {
            final var keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
            final var certKeyPair = keyGen.generateKeyPair();

            privateKeyEncryptedStorage.writeEncryptedKey(privateKeyPassword, certKeyPair.getPrivate());

            final var p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=" + principalName),
                    certKeyPair.getPublic()
            );

            final var names = new ArrayList<>(List.of(principalName));
            if (altNames != null) {
                names.addAll(altNames);
            }

            Extension subjectAltNames = new Extension(Extension.subjectAlternativeName, false,
                    new DEROctetString(
                            new GeneralNames(
                                    names.stream()
                                            .map(alternativeName -> new GeneralName(GeneralName.dNSName, alternativeName))
                                            .toArray(GeneralName[]::new)
                            )
                    )
            );
            p10Builder.addAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                    new Extensions(subjectAltNames));

            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SIGNING_ALGORITHM);
            ContentSigner signer = csBuilder.build(certKeyPair.getPrivate());
            return p10Builder.build(signer);

        } catch (Exception e) {
            throw new CSRGenerationException("Failed to generate Certificate Signing Request", e);
        }
    }
}
