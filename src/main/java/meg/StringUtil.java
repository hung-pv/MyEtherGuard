package meg;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

import org.apache.commons.lang3.StringUtils;

public class StringUtil {

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
	
	public static String toPathChars(String original) {
		return original.replaceAll("[^aA-zZ0-9_-]", "_");
	}
	
	public static String getSimpleCheckSum(String text) {
		if (StringUtils.isBlank(text)) return "0000";
		int sum = 0;
		for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
			sum+=b;
		}
		return toPathChars(String.valueOf(sum));
	}
	
	public static String getDomainName(String url) throws URISyntaxException {
	    URI uri = new URI(url);
	    String domain = uri.getHost();
	    return domain.startsWith("www.") ? domain.substring(4) : domain;
	}
}
