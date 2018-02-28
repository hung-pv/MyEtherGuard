package meg;

import java.io.File;
import java.nio.file.Paths;

public class USB {

	private static final String USB_ID_FILE_NAME = "usb.4uth";
	
	private File drive;

	public USB(File drive) {
		this.drive = drive;
	}
	
	public File getFileInUsb(String...path) {
		return this.drive == null ? null : Paths.get(this.drive.getAbsolutePath(), path).toFile();
	}
	
	public boolean isValid() {
		return this.drive != null && this.drive.exists() && this.drive.isDirectory() && this.getUsbIdFile().exists();
	}
	
	public String getAbsolutePath() {
		return this.drive == null ? null : this.drive.getAbsolutePath();
	}
	
	public File getUsbIdFile() {
		return getFileInUsb(USB_ID_FILE_NAME);
	}
}
