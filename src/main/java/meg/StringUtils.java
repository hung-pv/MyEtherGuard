package meg;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

public class StringUtils {

	public static byte[] getBytes(String text, int size) {
		return getBytes(text, size, StandardCharsets.UTF_8);
	}
	
	public static byte[] getBytes(String text, int size, Charset c) {
		byte[] barr = text.getBytes(c);
		byte[] key = new byte[size];
		if (barr.length > key.length) {
			System.arraycopy(barr, 0, key, 0, key.length);
		} else if (barr.length < key.length) {
			System.arraycopy(barr, 0, key, 0, barr.length);
		} else {
			key = barr;
		}
		return key;
	}
	
	public static void printArray(byte[] barr) {
		try (Formatter formatter = new Formatter()) {
			for (byte b : barr) {
				formatter.format("%02x", b);
			}
			System.out.println(formatter.toString());
		} catch (Exception e) {
			System.out.println("ERR");
		}
	}
}
