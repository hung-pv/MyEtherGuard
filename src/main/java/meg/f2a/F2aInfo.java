package meg.f2a;

import java.util.Base64;

import meg.App;
import meg.StringUtil;
import meg.UncompletedFile;

public class F2aInfo {

	private byte[] encrypted2faPrivateKey;
	private String account;
	private String productionName;
	private byte[] encryptedRemark;

	private F2aInfo() {

	}

	public F2aInfo(byte[] encrypted2faPrivateKey, String account, String productionName, byte[] encryptedRemark) {
		this.encrypted2faPrivateKey = encrypted2faPrivateKey;
		this.account = account;
		this.productionName = productionName;
		this.encryptedRemark = encryptedRemark;
	}

	public byte[] getEncrypted2faPrivateKey() {
		return this.encrypted2faPrivateKey;
	}

	public String getAccount() {
		return this.account;
	}

	public String getProductionName() {
		return this.productionName;
	}

	public byte[] getEncryptedRemark() {
		return this.encryptedRemark;
	}

	public boolean containsEncryptedRemark() {
		return this.encryptedRemark != null;
	}

	public String getFileName() {
		String shortProductionName = null;
		try {
			shortProductionName = StringUtil.getDomainName(this.productionName);
		} catch (Exception e) {
		}
		shortProductionName = StringUtil.toPathChars(shortProductionName == null ? this.productionName : shortProductionName);
		return String.format("%s.%s.%s.%s.%s", //
				StringUtil.toPathChars(this.account), //
				shortProductionName, //
				StringUtil.getSimpleCheckSum(this.account), //
				StringUtil.getSimpleCheckSum(shortProductionName), //
				App.FILE_2FA_EXT);
	}

	public byte[] toRaw() {
		UncompletedFile uf = new UncompletedFile();
		uf.append(this.encrypted2faPrivateKey == null ? null
				: Base64.getEncoder().encodeToString(this.encrypted2faPrivateKey));
		uf.append(this.account);
		uf.append(this.productionName);
		uf.append(this.encryptedRemark == null ? null : Base64.getEncoder().encodeToString(this.encryptedRemark));
		return uf.toRaw();
	}

	public static F2aInfo fromUncompletedFile(UncompletedFile file) {
		F2aInfo result = new F2aInfo();
		result.encrypted2faPrivateKey = file.getLine(0) == null ? null : Base64.getDecoder().decode(file.getLine(0));
		result.account = file.getLine(1);
		result.productionName = file.getLine(2);
		result.encryptedRemark = file.getLine(3) == null ? null : Base64.getDecoder().decode(file.getLine(3));
		return result;
	}
}
