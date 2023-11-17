package com.havo.cloud.doc.search.datastore.cloud.docs;

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
import com.havo.cloud.doc.search.datastore.transformer.GoogleDocumentTransformer;
import com.havo.cloud.doc.search.exception.CloudDocSearchException;
import com.havo.cloud.doc.search.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

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

@Slf4j
@Component
public class GoogleCloudDocs implements CloudDocs {
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
    public static final int PAGE_SIZE = 10;
    public static final String OFFLINE = "offline";
    private final GoogleCloudDocsConfigProperties googleCloudDocsConfigProperties;
    private Drive service;

    @Autowired
    public GoogleCloudDocs(GoogleCloudDocsConfigProperties googleCloudDocsConfigProperties) throws GeneralSecurityException, IOException {
        this.googleCloudDocsConfigProperties = googleCloudDocsConfigProperties;
        init();
    }

    /**
     * Init a new authorized API client service.
     */
    private void init() throws GeneralSecurityException, IOException {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        service = new Drive.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param httpTransport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Load client secrets.
        InputStream in = GoogleCloudDocs.class.getResourceAsStream(googleCloudDocsConfigProperties.getSecretPath());
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + googleCloudDocsConfigProperties.getSecretPath());
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType(OFFLINE)
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        //returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(googleCloudDocsConfigProperties.getUserId());
    }

    @Override
    public List<Document> listDocument() {
        List<File> files;
        try {
            FileList result = service.files().list()
                    .setPageSize(PAGE_SIZE)
                    .setFields("nextPageToken, files(id, name, webViewLink, trashed, version, mimeType)")
                    .execute();

            files = result.getFiles()
                    .stream()
                    .filter(file -> googleCloudDocsConfigProperties.getSupportedFiles().contains(file.getMimeType()))
                    .collect(Collectors.toList());

            var nextPageToken = result.getNextPageToken();
            while (StringUtils.isNotEmpty(nextPageToken)) {
                result = service.files().list()
                        .setPageToken(nextPageToken)
                        .setPageSize(PAGE_SIZE)
                        .setFields("nextPageToken, files(id, name, webViewLink, trashed, version, mimeType)")
                        .execute();
                files.addAll(result.getFiles()
                        .stream()
                        .filter(file -> googleCloudDocsConfigProperties.getSupportedFiles().contains(file.getMimeType()))
                        .collect(Collectors.toList()));

                nextPageToken = result.getNextPageToken();
            }
        } catch (IOException e) {
            var msg = "Failure while trying to get the list of documents from the cloud storage.";
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
        return files.parallelStream()
                .map(GoogleDocumentTransformer::toDocument)
                .collect(Collectors.toList());
    }

    @Override
    public String getDocumentContent(String documentId) {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();
        try {
            parser.parse(getDocumentContentStream(documentId), handler, metadata, new ParseContext());
        } catch (IOException | SAXException |  TikaException e) {
            String msg = "Failed to parse the content for the document with id " + documentId;
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
        return handler.toString();
    }

    private ByteArrayInputStream getDocumentContentStream(String documentId) {
        var outputStream = new ByteArrayOutputStream();
        try {
            service.files().get(documentId).executeMediaAndDownloadTo(outputStream);
        } catch (IOException e) {
            String msg = "Failed to read the content for the document with id " + documentId;
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    @Override
    public boolean isDocumentPresent(String documentId) {
        try {
            service.files().get(documentId).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND)
                return false;
        } catch (IOException e) {
            String msg = "Failed to get the document with id " + documentId;
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
        return true;
    }

}
