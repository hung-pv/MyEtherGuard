/*-
 * Based on sample code at
 * https://www.programcreek.com/java-api-examples/index.php?api=org.bouncycastle.crypto.paddings.PKCS7Padding
 */

package meg.crypto;

import java.util.Arrays;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import meg.StringUtil;

public class AES256 {
	public static byte[] encrypt(byte[] data, byte[] key, String iv) {
		return process(data, key, StringUtil.getBytes(iv, 16), true);
	}

	public static byte[] decrypt(byte[] data, byte[] key, String iv) {
		return process(data, key, StringUtil.getBytes(iv, 16), false);
	}

	private static byte[] process(byte[] data, byte[] key, byte[] iv, boolean isEncrypt) {
		KeyParameter keyParam = new KeyParameter(key);
		CipherParameters params = new ParametersWithIV(keyParam, iv);
		BlockCipherPadding padding = new PKCS7Padding();
		BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
		cipher.reset();
		cipher.init(isEncrypt, params);
		byte[] buffer = new byte[cipher.getOutputSize(data.length)];
		int len = cipher.processBytes(data, 0, data.length, buffer, 0);
		try {
			len += cipher.doFinal(buffer, len);
		} catch (Exception e) {
			throw new CryptoException(e);
		}
		return Arrays.copyOfRange(buffer, 0, len);
	}
	
	private byte[] key, iv;

	public AES256(byte[] key, String iv) {
		this.key = key;
		this.iv = StringUtil.getBytes(iv, 16);
	}
	
	public byte[] encrypt(byte[] data) {
		return AES256.process(data, this.key, this.iv, true);
	}
	
	public byte[] encryptNullable(byte[] data) {
		if (data == null) return null;
		return encrypt(data);
	}
	
	public byte[] decrypt(byte[] data) {
		return AES256.process(data, this.key, this.iv, false);
	}
	
	public byte[] decryptNullable(byte[] data) {
		if (data == null) return null;
		return decrypt(data);
	}
}