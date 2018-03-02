package meg;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

/** -
 * Never override file
 * file will be marked as read-only
 */
public class UniqueFileUtils {
	
	public static void write(File file, CharSequence cs) throws IOException {
		checkBeforeWrite(file);
		FileUtils.write(file, cs, StandardCharsets.UTF_8, false);
		afterWrote(file);
	}
	
	public static void write(File file, byte[] data) throws IOException {
		checkBeforeWrite(file);
		FileUtils.writeByteArrayToFile(file, data, false);
		afterWrote(file);
	}
	
	private static void checkBeforeWrite(File file) throws FileExistsException {
		if (file.exists()) {
			throw new FileExistsException("File is already exists, can not be over-written");
		}
	}
	
	private static void afterWrote(File file) {
		try {
			file.setReadOnly();
		} catch (Exception e) {
		}
	}
}
