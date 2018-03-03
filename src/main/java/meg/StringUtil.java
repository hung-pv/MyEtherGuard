package meg;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

public class StringUtils {

	public static byte[] getBytes(String text, int size) {
		return getBytes(text, size, StandardCharsets.UTF_8);
	}

	public static byte[] getBytesNullable(String text) {
		if (text == null) return null;
		return text.getBytes(StandardCharsets.UTF_8);
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
	
	public static String beautiNumber(String number) {
		String natural, decimal;
		if (number.contains(".")) {
			natural = number.substring(0, number.indexOf("."));
			decimal = number.substring(number.indexOf("."));
		} else {
			natural = number;
			decimal = "";
		}
		
		char[] reverse = org.apache.commons.lang3.StringUtils.reverse(natural).toCharArray();
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		for (char digit : reverse) {
			sb.append(digit);
			if (counter%3 == 2) {
				sb.append(',');
			}
			counter++;
		}
		
		String result = org.apache.commons.lang3.StringUtils.reverse(sb.toString()) + decimal;
		if (result.startsWith(",")) {
			result = result.substring(1);
		}
		return result;
	}
}
