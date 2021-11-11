package name.qd.tp2.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class GoogleDriveUtils {
	private static final Logger log = LoggerFactory.getLogger(GoogleDriveUtils.class);
	private String applicationName;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_FOLDER_PATH = "googledrive/tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    
    private String workingPath;
    private String credentialsFilePath;
    private Drive drive;

    public GoogleDriveUtils(String applicationName, String credentialsFilePath, String workingPath) {
    	this.applicationName = applicationName;
    	this.credentialsFilePath = credentialsFilePath;
    	this.workingPath = workingPath;
    	
    	initDrive();
    }
    
    private void initDrive() {
		try {
			NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
	                .setApplicationName(applicationName)
	                .build();
		} catch (GeneralSecurityException | IOException e) {
			log.error("Init google drive failed.", e);
		}
    }
    
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(new FileInputStream(credentialsFilePath)));
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_FOLDER_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    // 開機步驟
    // 檢查local是否有當日交易紀錄檔
    // 到google drive找交易紀錄檔
    // 有就下載 沒有就create
    // 有新的交易 就寫local 並update google drive
    
    public boolean isFileExist(String fileName, String folderId) {
    	return getFileId(fileName, folderId) != null;
    }
    
    public String getFileId(String fileName, String folderId) {
    	FileList fileList = null;
		try {
			fileList = drive.files().list().setQ("name='"+fileName+"' and parents in '"+folderId+"'").execute();
		} catch (IOException e) {
			log.error("query file from google drive failed. fileName: {}, folderId: {}", fileName, folderId);
		}
		if(fileList != null) {
			if(fileList.getFiles().size() == 1) {
				return fileList.getFiles().get(0).getId();
			}
		}
		return null;
    }
    
    public String downloadFileByName(String fileName, String folderId) {
    	try {
			FileList fileList = drive.files().list().setQ("name='"+fileName+"' and parents in '"+folderId+"'").execute();
			if(fileList.getFiles().size() == 1) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				drive.files().get(fileList.getFiles().get(0).getId()).executeMediaAndDownloadTo(outputStream);
				if(!Files.exists(Paths.get(workingPath))) {
					Files.createDirectory(Paths.get(workingPath));
				}
				Path path = Paths.get(workingPath, fileName);
				Files.createFile(path);
				FileOutputStream fos = new FileOutputStream(path.toFile());
				outputStream.writeTo(fos);
				
				return fileList.getFiles().get(0).getId();
			}
		} catch (IOException e) {
			log.error("download file from google drive failed.", e);
		}
    	return null;
    }
    
    public boolean updateFile(String filePath, String fileId, String folderId, String mimeType) {
    	Path path = Paths.get(filePath);
    	
    	File file = new File();
        file.setName(path.getFileName().toString());
        file.setMimeType(mimeType);
        
        java.io.File fileContent = new java.io.File(filePath);
        FileContent mediaContent = new FileContent(mimeType, fileContent);

		try {
			File fileUploaded = drive.files().update(fileId, file, mediaContent).execute();
			if(fileUploaded != null) {
				return true;
			}
		} catch (IOException e) {
			log.error("Update file to google drive failed.", e);
		}
		return false;
    }
    
    public boolean uploadFile(String filePath, String folderId, String mimeType) {
    	Path path = Paths.get(filePath);
    	
    	File file = new File();
        file.setName(path.getFileName().toString());
        file.setMimeType(mimeType);
        if(!"".equals(folderId)) {
        	List<String> lstFolderId = new ArrayList<>();
            lstFolderId.add(folderId);
            file.setParents(lstFolderId);
        }
        
        java.io.File fileContent = new java.io.File(filePath);
        FileContent mediaContent = new FileContent(mimeType, fileContent);

		try {
			File fileUploaded = drive.files().create(file, mediaContent).execute();
			if(fileUploaded != null) {
				return true;
			}
		} catch (IOException e) {
			log.error("Upload file to google drive failed.", e);
		}
		return false;
    }
}
