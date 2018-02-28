package meg.crypto;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES128 {
	private static final String AES_SPEC = "AES/CBC/PKCS5Padding";
	private static final String AES = "AES";

	public static byte[] encrypt(byte[] data, String salt) throws Exception {
		byte[] key = getKey(salt);
		SecretKeySpec secretKey = new SecretKeySpec(key, AES);
		Cipher cipher = Cipher.getInstance(AES_SPEC);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(key));
		return cipher.doFinal(data);
	}

	public static byte[] decrypt(byte[] cipherText, String salt) throws Exception {
		byte[] key = getKey(salt);
		SecretKeySpec secretKey = new SecretKeySpec(key, AES);
		Cipher cipher = Cipher.getInstance(AES_SPEC);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(key));
		return cipher.doFinal(cipherText);
	}

	private static byte[] getKey(String text) {
		byte[] barr = text.getBytes(StandardCharsets.UTF_8);
		byte[] key = new byte[16];
		if (barr.length > 16) {
			System.arraycopy(barr, 0, key, 0, 16);
		} else if (barr.length < 16) {
			System.arraycopy(barr, 0, key, 0, barr.length);
		} else {
			key = barr;
		}
		return key;
	}
}
