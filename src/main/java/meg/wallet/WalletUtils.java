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
        BigInteger pk = new BigInteger(privateKey, 16);
        ECKey key = ECKey.fromPrivate(pk);
        return key.getAddress();
	}
	
	public static WalletInfo readWalletInfo(File file) throws IOException {
		List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
		if (lines.size() < 3) {
			throw new IOException("Invalid format of wallet file " + file.getName());
		}
		WalletInfo wi = new WalletInfo(lines.get(0), lines.get(1), Base64.getDecoder().decode(lines.get(2)));
		return wi;
	}
}
