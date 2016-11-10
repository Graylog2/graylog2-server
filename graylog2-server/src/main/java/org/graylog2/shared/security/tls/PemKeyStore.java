/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.security.tls;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public final class PemKeyStore {
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    /**
     * Generates a key specification for an (encrypted) private key.
     *
     * @param password characters, if {@code null} or empty an unencrypted key is assumed
     * @param key      bytes of the DER encoded private key
     * @return a key specification
     * @throws IOException                        if parsing {@code key} fails
     * @throws NoSuchAlgorithmException           if the algorithm used to encrypt {@code key} is unkown
     * @throws NoSuchPaddingException             if the padding scheme specified in the decryption algorithm is unkown
     * @throws InvalidKeySpecException            if the decryption key based on {@code password} cannot be generated
     * @throws InvalidKeyException                if the decryption key based on {@code password} cannot be used to decrypt
     *                                            {@code key}
     * @throws InvalidAlgorithmParameterException if decryption algorithm parameters are somehow faulty
     */
    protected static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidKeyException, InvalidAlgorithmParameterException {

        if (password == null || password.length == 0) {
            return new PKCS8EncodedKeySpec(key);
        }

        final EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
        final PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        final SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

        @SuppressWarnings("InsecureCryptoUsage")
        final Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }

    /**
     * Generates a new {@link KeyStore}.
     *
     * @param certChainFile    a X.509 certificate chain file in PEM format,
     * @param keyFile          a PKCS#8 private key file in PEM format,
     * @param keyPasswordChars the password of the {@code keyFile}.
     *                         {@code null} if it's not password-protected.
     * @return generated {@link KeyStore}.
     */
    @SuppressWarnings("InsecureCryptoUsage")
    public static KeyStore buildKeyStore(Path certChainFile, Path keyFile, char[] keyPasswordChars)
            throws KeyStoreException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException,
            CertificateException, KeyException, IOException {
        char[] password = keyPasswordChars == null ? EMPTY_CHAR_ARRAY : keyPasswordChars;

        byte[] encodedKey = PemReader.readPrivateKey(keyFile);
        final PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPasswordChars, encodedKey);

        PrivateKey key;
        try {
            key = KeyFactory.getInstance("RSA").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore) {
            try {
                key = KeyFactory.getInstance("DSA").generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore2) {
                try {
                    key = KeyFactory.getInstance("EC").generatePrivate(encodedKeySpec);
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked", e);
                }
            }
        }

        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final List<byte[]> certs = PemReader.readCertificates(certChainFile);
        final List<Certificate> certChain = new ArrayList<>(certs.size());

        for (byte[] buf : certs) {
            certChain.add(cf.generateCertificate(new ByteArrayInputStream(buf)));
        }

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, password);
        ks.setKeyEntry("key", key, password, certChain.toArray(new Certificate[certChain.size()]));

        return ks;
    }
}
