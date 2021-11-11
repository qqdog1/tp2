package name.qd.tp2.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleDriveUtilsTest {
	private GoogleDriveUtils googleDriveUploader;
	
	@BeforeEach
	void init() {
		googleDriveUploader = new GoogleDriveUtils("uploadTest", "C:/qqdog1/tp2/config/credentials.json", "./googledrive/tmp");
	}
	
	@Test
	public void testUploadFolder() {
		googleDriveUploader.uploadFile("C:/qqdog1/tp2/abc", "1pJZZ33Biqlcs8oWOqIplDPvYe1nbmuHL", "application/zip");
	}
	
	@Test
	public void testDownloadFile() {
		googleDriveUploader.downloadFileByName("abc.txt", "1Y42XGdSj1tjWmmGk5XCAg5MlFbq0tZ2E");
//		googleDriveUploader.downloadFileByName("abc.txt", "1pJZZ33Biqlcs8oWOqIplDPvYe1nbmuHL");
	}
	
	@Test
	public void testIsFileExist() {
		String fileId = googleDriveUploader.getFileId("abc.txt", "1Y42XGdSj1tjWmmGk5XCAg5MlFbq0tZ2E");
		System.out.println(fileId);
	}
	
	@Test
	public void testUpdateFile() {
		String fileId = googleDriveUploader.getFileId("abc.txt", "1Y42XGdSj1tjWmmGk5XCAg5MlFbq0tZ2E");
		
		try {
			Files.writeString(Paths.get("./googledrive/tmp/abc.txt"), "GGAA" + System.lineSeparator(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		googleDriveUploader.updateFile("./googledrive/tmp/abc.txt", fileId, "1Y42XGdSj1tjWmmGk5XCAg5MlFbq0tZ2E", "text/plain");
	}
}
