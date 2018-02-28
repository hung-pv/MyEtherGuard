package meg.crypto;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

import meg.CollectionUtils;

public class RSA {
	private static final String RSA_SPEC = "RSA/ECB/PKCS1Padding";
	private static final String RSA = "RSA";
	private static final int RSA_MAX_SRC_SZ = 245;
	private static final int RSA_ENCRYPTED_LENGTH = 256;

	private static Cipher cipherEncrypt = null;
	private static Cipher cipherDecrypt = null;

	public static RSAKeyPair createKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
			keyGen.initialize(2048);
			KeyPair kp = keyGen.genKeyPair();
			return new RSAKeyPair(kp.getPublic(), kp.getPrivate());
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static void setKey(PublicKey publicKey, PrivateKey privateKey) throws Exception {
		cipherEncrypt = Cipher.getInstance(RSA_SPEC);
		cipherEncrypt.init(Cipher.ENCRYPT_MODE, publicKey);
		cipherDecrypt = Cipher.getInstance(RSA_SPEC);
		cipherDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
	}

	public static boolean isKeysSet() {
		if (cipherEncrypt == null) {
			return false;
		}
		if (cipherDecrypt == null) {
			return false;
		}
		return true;
	}

	public static byte[] encryptRSA(byte[] src) throws Exception {
		List<byte[]> spl = CollectionUtils.split(src, RSA_MAX_SRC_SZ);
		List<byte[]> encryptedL = new ArrayList<>();

		int totalSize = 0;
		synchronized (cipherEncrypt) {
			for (int i = 0; i < spl.size(); i++) {
				byte[] encrypted = cipherEncrypt.doFinal(spl.get(i));
				if (encrypted.length != RSA_ENCRYPTED_LENGTH) {
					throw new RuntimeException("Invalid encrypted data length. Expected " + RSA_ENCRYPTED_LENGTH
							+ " but " + encrypted.length);
				}
				totalSize += encrypted.length;
				encryptedL.add(encrypted);
			}
		}
		return CollectionUtils.merge(encryptedL, totalSize);
	}

	public static byte[] decryptRSA(byte[] src) throws Exception {
		List<byte[]> blocks = CollectionUtils.split(src, RSA_ENCRYPTED_LENGTH);
		List<byte[]> decryptedL = new ArrayList<>();
		int totalSize = 0;
		synchronized (cipherDecrypt) {
			for (byte[] block : blocks) {
				try {
					byte[] decrypted = cipherDecrypt.doFinal(block);
					decryptedL.add(decrypted);
					totalSize += decrypted.length;
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
		return CollectionUtils.merge(decryptedL, totalSize);
	}

	public static PublicKey convertPublicKey(byte[] data) throws Exception {
		KeyFactory kf1 = KeyFactory.getInstance(RSA);
		return kf1.generatePublic(new X509EncodedKeySpec(data));
	}

	public static PrivateKey convertPrivateKey(byte[] data) throws Exception {
		KeyFactory kf1 = KeyFactory.getInstance(RSA);
		return kf1.generatePrivate(new PKCS8EncodedKeySpec(data));
	}
}
