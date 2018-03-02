package meg.wallet;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

public class WalletUtils {
	public static String getFriendlyEthAddressFromPrivateKey(String privateKey) {
        return String.format("0x%s", Hex.toHexString(getEthAddressFromPrivateKey(privateKey)));
	}

	public static byte[] getEthAddressFromPrivateKey(String privateKey) {
        return getECKey(privateKey).getAddress();
	}
	
	public static ECKey getECKey(String privateKey) {
		return ECKey.fromPrivate(new BigInteger(privateKey, 16));
	}
	
	public static WalletInfo readWalletInfo(File file) throws IOException {
		List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
		if (lines.size() < 5) {
			throw new IOException("Invalid format of wallet file " + file.getName());
		}
		
		String mnemoticInfo = lines.get(3);
		byte[] mnemoticEncrypted = null;
		if (!mnemoticInfo.trim().startsWith("(")) {
			mnemoticEncrypted = Base64.getDecoder().decode(mnemoticInfo.trim());
		}
		
		String noteInfo = lines.get(4);
		byte[] noteEncrypted = null;
		if (!noteInfo.trim().startsWith("(")) {
			noteEncrypted = Base64.getDecoder().decode(noteInfo.trim());
		}
		
		WalletInfo wi = new WalletInfo(lines.get(0), lines.get(1), Base64.getDecoder().decode(lines.get(2)), mnemoticEncrypted, noteEncrypted);
		return wi;
	}
}
