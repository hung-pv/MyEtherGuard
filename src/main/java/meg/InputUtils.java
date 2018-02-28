package meg;

import java.util.Scanner;

public class InputUtils {

	private static final Scanner in = new Scanner(System.in);
	private static boolean autoTrim = true;
	
	public static void setAutoTrim(boolean autoTrim) {
		InputUtils.autoTrim = autoTrim;
	}
	
	public static String getRawInput() {
		String input = in.nextLine();
		return autoTrim ? input.trim() : input;
	}
	
	public static String getInput(int maxLength) {
		String input = in.nextLine();
		if (input != null) {
			input = input.trim();
			if (input.length() > maxLength) {
				input = null;
			}
		}
		return autoTrim ? org.apache.commons.lang3.StringUtils.trimToNull(input) : input;
	}
	
	public static boolean confirm(String msg) {
		o(msg);
		return "y".equalsIgnoreCase(getInput(1));
	}

	public static String getPassword(String msg) {
		o(msg);
		String input = null;
		do {
			input = org.apache.commons.lang3.StringUtils.trimToNull(getRawInput());
			if (input.length() > 16) {
				o("NOTICE: Your password is longer than 16 characters, will be cut off to the first 16 chars only");
				input = input.substring(0, 16);
			}
		} while (input == null);
		return input;
	}

	private static void o(String pattern, Object... params) {
		System.out.println(String.format(pattern, params));
	}

	@SuppressWarnings("unused")
	private static void e(String pattern, Object... params) {
		System.err.println(String.format(pattern, params));
	}
}
