package meg.wallet;

import java.util.Base64;

public class WalletInfo {

	private String type;
	private String address;
	private byte[] privateKeyEncrypted;

	public WalletInfo(String type, String address, byte[] privateKeyEncrypted) {
		this.type = type;
		this.address = address;
		this.privateKeyEncrypted = privateKeyEncrypted;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public byte[] getPrivateKeyEncrypted() {
		return privateKeyEncrypted;
	}

	public void setPrivateKeyEncrypted(byte[] privateKeyEncrypted) {
		this.privateKeyEncrypted = privateKeyEncrypted;
	}

	@Override
	public String toString() {
		return (this.type == null ? "other" : this.type) + "\n" + //
				this.address + "\n" + //
				Base64.getEncoder().encodeToString(this.privateKeyEncrypted);
	}
}
