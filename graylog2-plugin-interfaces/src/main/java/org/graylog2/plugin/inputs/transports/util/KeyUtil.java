package org.graylog2.plugin.inputs.transports.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.velocity.util.EnumerationIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

public class KeyUtil {

	private static final Logger LOG = LoggerFactory.getLogger(KeyUtil.class);
	private static final Joiner JOINER = Joiner.on(",").skipNulls();
	private static final Joiner JOINER_U = Joiner.on("_").skipNulls();
	private static final Pattern KEY_REGEX = Pattern.compile("-{5}BEGIN (?:(RSA|DSA)? )?(ENCRYPTED )?PRIVATE KEY-{5}\\r?\\n([A-Z0-9a-z+/\\r\\n]+={0,2})\\r?\\n-{5}END (?:(?:RSA|DSA)? )?(?:ENCRYPTED )?PRIVATE KEY-{5}\\r?\\n$", Pattern.MULTILINE);

	public static TrustManager[] initTrustStore(File tlsClientAuthCertFile)
			throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(null, null);
		loadCertificates(trustStore, tlsClientAuthCertFile, CertificateFactory.getInstance("X.509"));
		LOG.info("TrustStore: " + trustStore + " aliases: " + join(trustStore.aliases()));
		TrustManagerFactory instance = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		instance.init(trustStore);
		return instance.getTrustManagers();
	}

	protected static void loadCertificates(KeyStore trustStore, File file, CertificateFactory cf)
			throws CertificateException, KeyStoreException, IOException {
		if (file.isFile()) {
			List<Certificate> certChain = Lists.newArrayList(cf.generateCertificates(new FileInputStream(file)));
			for (int i = 0; i < certChain.size(); i++) {
				Certificate cert = certChain.get(i);
				trustStore.setCertificateEntry(JOINER_U.join(file.getAbsolutePath(), i), cert);
				LOG.debug("adding certificate to truststore:", cert.toString());
			}
		} else if (file.isDirectory()) {
			for (Path f : Files.newDirectoryStream(file.toPath())) {
				loadCertificates(trustStore, f.toFile(), cf);
			}

		}
	}

	public static KeyManager[] initKeyStore(File tlsKeyFile, File tlsCertFile, String tlsKeyPassword)
			throws FileNotFoundException, IOException, GeneralSecurityException {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null, null);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> certChain = cf.generateCertificates(new FileInputStream(tlsCertFile));

		PrivateKey pk = loadPrivateKey(tlsKeyFile, tlsKeyPassword);
		char[] password = Strings.isNullOrEmpty(tlsKeyPassword) ? new char[0] : tlsKeyPassword.toCharArray();
		ks.setKeyEntry("key", pk, password, certChain.toArray(new Certificate[certChain.size()]));
		LOG.info("KeyStore: " + ks + " aliases: " + join(ks.aliases()));
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, password);
		return kmf.getKeyManagers();
	}

	private static String join(Enumeration<String> aliases) {
		return JOINER.join(new EnumerationIterator(aliases));
	}

	protected static PrivateKey loadPrivateKey(File file, String password)
			throws IOException, GeneralSecurityException {
		PrivateKey key = null;
		try (InputStream is = new FileInputStream(file)) {
			byte[] keyBytes = ByteStreams.toByteArray(is);
			String keyString = new String(keyBytes, StandardCharsets.US_ASCII);
			Matcher m = KEY_REGEX.matcher(keyString);
			byte[] encoded = keyBytes;
			String algorithm = "RSA";
			EncodedKeySpec keySpec;
			if (m.matches()) {
				if (!Strings.isNullOrEmpty(m.group(1))) {
					throw new IllegalArgumentException("unsupported key type PCKS1 please convert to PCKS8");
				}
				encoded = Base64.getDecoder().decode(m.group(3).replaceAll("[\\r\\n]", ""));
				keySpec = createKeySpec(encoded, password);

			} else {
				keySpec = createKeySpec(encoded, password);
			}
			if (keySpec == null) {
				throw new IllegalArgumentException("unsupported key type");
			}
			KeyFactory kf = KeyFactory.getInstance(algorithm);
			key = kf.generatePrivate(keySpec);
		}
		return key;
	}

	private static PKCS8EncodedKeySpec createKeySpec(byte[] keyBytes, String password)
			throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException {
		if (Strings.isNullOrEmpty(password)) {
			return new PKCS8EncodedKeySpec(keyBytes);
		}
		EncryptedPrivateKeyInfo pkInfo = new EncryptedPrivateKeyInfo(keyBytes);
		SecretKeyFactory kf = SecretKeyFactory.getInstance(pkInfo.getAlgName());
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKey sk = kf.generateSecret(keySpec);
		Cipher cipher = Cipher.getInstance(pkInfo.getAlgName());
		cipher.init(Cipher.DECRYPT_MODE, sk, pkInfo.getAlgParameters());
		return pkInfo.getKeySpec(cipher);
	}

}
