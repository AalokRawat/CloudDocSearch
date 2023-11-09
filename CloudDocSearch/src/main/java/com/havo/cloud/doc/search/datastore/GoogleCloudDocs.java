package com.havo.cloud.doc.search.datastore;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoogleCloudDocs {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Cloud Document Search";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    private DocsConfigProperties docsConfigProperties;
    private Drive service;

    @Autowired
    public GoogleCloudDocs(DocsConfigProperties docsConfigProperties) throws GeneralSecurityException, IOException {
        this.docsConfigProperties = docsConfigProperties;
        init();
    }

    // Build a new authorized API client service.
    private void init() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleCloudDocs.class.getResourceAsStream(docsConfigProperties.getSecretPath());
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + docsConfigProperties.getSecretPath());
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize(docsConfigProperties.getUserId());

        //returns an authorized Credential object.
        return credential;
    }

    /**
     * Search for specific set of files.
     *
     * @return search result list.
     * @throws IOException if service account credentials file not found.
     */
    public List<File> listFiles() throws IOException {
        List<File> files = new ArrayList<>();

        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, webViewLink, trashed, version, mimeType)")
                .execute();

        files.addAll(result.getFiles()
                .stream()
                .filter(file -> docsConfigProperties.getSupportedFiles().contains(file.getMimeType()))
                .collect(Collectors.toList()));

        var nextPageToken = result.getNextPageToken();
        while (StringUtils.isNotEmpty(nextPageToken)) {
            result = service.files().list()
                    .setPageToken(nextPageToken)
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name, webViewLink, trashed, version, mimeType)")
                    .execute();
            files.addAll(result.getFiles()
                    .stream()
                    .filter(file -> docsConfigProperties.getSupportedFiles().contains(file.getMimeType()))
                    .collect(Collectors.toList()));

            nextPageToken = result.getNextPageToken();
        }
        return files;
    }

    public InputStream getFileContent(String fileId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public boolean isFilePresent(String fileId) throws IOException {
        try {
            service.files().get(fileId).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND)
                return false;
        }
        return true;
    }

}
