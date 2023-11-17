package com.havo.cloud.doc.search.datastore.cloud.docs;

import com.havo.cloud.doc.search.model.Document;

import java.util.List;

public interface CloudDocs {

    List<Document> listDocument();

    String getDocumentContent(String documentId);

    boolean isDocumentPresent(String documentId);
}
