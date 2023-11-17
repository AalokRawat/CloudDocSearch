package com.havo.cloud.doc.search.datastore.transformer;

import com.google.api.services.drive.model.File;
import com.havo.cloud.doc.search.model.Document;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GoogleDocumentTransformer {

    public static Document toDocument(File file) {
        return new Document()
                .id(file.getId())
                .name(file.getName())
                .deleted(file.getTrashed())
                .version(file.getVersion())
                .path(file.getWebViewLink())
                .type(getDocType(file.getMimeType()));
    }

    private static String getDocType(String mimeType) {
        if("text/plain".equalsIgnoreCase(mimeType)) {
            return "text";
        }
        if("application/pdf".equalsIgnoreCase(mimeType)) {
            return "pdf";
        }
        if("text/csv".equalsIgnoreCase(mimeType)) {
            return "csv";
        }
        if("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(mimeType)) {
            return "docx";
        }
        return "not supported";
    }
}
