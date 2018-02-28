package meg.crypto;

import java.security.Key;
import java.util.Base64;

public class RSAKeyPair {
	private Key publicKey;
	private Key privateKey;

	public RSAKeyPair(Key publicKey, Key privateKey) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}

	public Key getPublicKey() {
		return this.publicKey;
	}

	public byte[] getPublicKeyRaw() {
		return this.publicKey.getEncoded();
	}

	public String getPublicKeyAsString() {
		return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
	}

	public Key getPrivateKey() {
		return this.privateKey;
	}

	public byte[] getPrivateKeyRaw() {
		return this.privateKey.getEncoded();
	}

	public String getPrivateKeyAsString() {
		return Base64.getEncoder().encodeToString(this.privateKey.getEncoded());
	}
}
