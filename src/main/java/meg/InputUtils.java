package meg;

import java.io.Console;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class InputUtils {

	private static final Console csl = System.console();
	private static final Scanner in = new Scanner(System.in);

	public static String getRawInput(String ask) {
		if (csl == null) {
			if (ask != null) o(ask);
			return StringUtils.trimToEmpty(in.nextLine());
		}
		return ask == null ? csl.readLine() : csl.readLine(ask);
	}

	public static String getInput(String ask, int maxLength) {
		String input;
		if (csl != null) {
			input = ask == null ? csl.readLine() : csl.readLine(ask);
		} else {
			if (ask != null) o(ask);
			input = in.nextLine();
		}
		if (input != null) {
			input = input.trim();
			if (input.length() > maxLength) {
				input = null;
			}
		}
		return StringUtils.trimToNull(input);
	}

	public static boolean confirm(String msg) {
		o(msg);
		return "y".equalsIgnoreCase(getInput("Y/N: ", 1));
	}

	public static String getPassword(String msg) {
		String input;
		if (csl == null) {
			input = getRawInput(msg);
		} else {
			char[] cinput = msg == null ? csl.readPassword() : csl.readPassword(msg);
			if (cinput == null)
				return null;
			input = String.valueOf(cinput);			
		}
		if (StringUtils.isBlank(input))
			return null;
		return input;
	}

	public static String getPassword_required(String msg, int min_length) {
		String pwd = getPassword(msg);
		while (true) {
			if (pwd == null) {
				o("Can not be empty");
				pwd = getPassword(msg == null ? "Again: " : msg);
				continue;
			}
			if (pwd.length() < min_length) {
				o("Minumum length required is %d chars", min_length);
				pwd = getPassword(msg == null ? "Again: " : msg);
				continue;
			}
			break;
		}
		if (pwd.length() > 16) {
			o("NOTICE: Your password is longer than 16 characters, will be cut off to the first 16 chars only");
			pwd = pwd.substring(0, 16);
		}
		return pwd;
	}

	public static int getInt(String ask) {
		String input = getRawInput(ask);
		try {
			return Integer.parseInt(input);
		} catch (Exception e) {
			o("NOT a valid number!");
			return getInt(ask == null ? "Again: " : ask);
		}
	}

	public static String getInput2faPrivateKey() {
		return getInput("2fa private key", true, "^[aA-zZ0-9]{4,}$",
				"Alphabet and numeric only, more than 4 characters", null);
	}

	@SuppressWarnings("unchecked")
	public static <RC> RC getInput(String name, boolean blankable, String regexPattern, String descripbleRegexPattern,
			IConvert<RC> converter) {
		String input;

		while (true) {
			input = getRawInput(null);
			if (!blankable && StringUtils.isBlank(input)) {
				if (name == null) {
					o("Could not be empty, try again:");
				} else {
					o("%s could not be empty, try again:", name);
				}
				continue;
			}
			if (regexPattern != null && regexPattern.length() > 0) {
				if (!Pattern.matches(regexPattern, input)) {
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

		return (RC) input;
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
