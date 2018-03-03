package meg;

import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
		o("Y/N");
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

	public static int getInt() {
		String input;
		while (true) {
			input = org.apache.commons.lang3.StringUtils.trimToNull(getRawInput());
			try {
				return Integer.parseInt(input);
			} catch (Exception e) {
				o("Invalid number!");
				o("Try again:");
				continue;
			}
		}
	}
	
	public static String getInput2faPrivateKey() {
		return getInput("2fa private key", true, "^[aA-zZ0-9]{4,}$", "Alphabet and numeric only, more than 4 characters", null);
	}
	
	@SuppressWarnings("unchecked")
	public static <RC> RC getInput(String name, boolean blankable, String regexPattern, String descripbleRegexPattern, IConvert<RC> converter) {
		String input;
		
		while (true) {
			input = getRawInput();
			if (!blankable && StringUtils.isBlank(input)) {
				if (name == null) {
					o("Could not be empty, try again:");
				} else {
					o("%s could not be empty, try again:", name);
				}
				continue;
			}
			if (regexPattern != null && regexPattern.length() > 0) {
				if (!Pattern.matches(regexPattern,  input)) {
					String additinalInfo = name == null ? "" : " of " + name;
					if (descripbleRegexPattern == null) {
						o("Invalid format%s, please try again:", additinalInfo);
					} else {
						o("Invalid format%s !!!", additinalInfo);
						o(descripbleRegexPattern);
						o("Please try again:");
					}
					continue;
				}
			}
			break;
		}
		
		if (converter != null) {
			return converter.convert(input);
		}
		
		return (RC)input;
	}
	
	/*-
	public static void pressAnyKeyToContinue(String... args) {
		for (String arg : args) {
			o(arg);
		}
		if (args.length == 0) {
			o("Press any key to continue...");
		}
		try {
			System.in.read();
		} catch (Exception e) {
		}
	}
	*/

	private static void o(String pattern, Object... params) {
		System.out.println(String.format(pattern, params));
	}

	@SuppressWarnings("unused")
	private static void e(String pattern, Object... params) {
		System.err.println(String.format(pattern, params));
	}
}
