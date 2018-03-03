package meg.wallet;

import java.math.BigInteger;

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
}
