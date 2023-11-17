package com.havo.cloud.doc.search.datastore.index;

import com.havo.cloud.doc.search.model.Document;
import com.havo.cloud.doc.search.model.Response;

import java.util.List;

public interface IndexStorage {

    Response getDocument(String documentId);

    void updateDocument(Document document, String data);

    List<String> searchDocuments(String searchText);

    void deleteDocument(String documentId);
}
