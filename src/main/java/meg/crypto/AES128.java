package meg.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import meg.StringUtils;

public class AES128 {
	private static final String AES_SPEC = "AES/CBC/PKCS5Padding";
	private static final String AES = "AES";

	public static byte[] encrypt(byte[] data, String key) throws Exception {
		return encrypt(data, StringUtils.getBytes(key, 16));
	}

	public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
		SecretKeySpec secretKey = new SecretKeySpec(key, AES);
		Cipher cipher = Cipher.getInstance(AES_SPEC);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(key));
		return cipher.doFinal(data);
	}

	public static byte[] decrypt(byte[] data, String key) throws Exception {
		return decrypt(data, StringUtils.getBytes(key, 16));
	}

	public static byte[] decrypt(byte[] cipherText, byte[] key) throws Exception {
		SecretKeySpec secretKey = new SecretKeySpec(key, AES);
		Cipher cipher = Cipher.getInstance(AES_SPEC);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(key));
		return cipher.doFinal(cipherText);
	}
}
