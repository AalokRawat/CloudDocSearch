package com.havo.cloud.doc.search.service;

import com.havo.cloud.doc.search.datastore.cloud.docs.CloudDocs;
import com.havo.cloud.doc.search.datastore.index.ElasticIndexStore;
import com.havo.cloud.doc.search.datastore.cloud.docs.GoogleCloudDocs;
import com.havo.cloud.doc.search.datastore.index.IndexStorage;
import com.havo.cloud.doc.search.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CloudDocSearchProcessorImpl implements CloudDocSearchProcessor {
    private final IndexStorage indexStorage;
    private final CloudDocs cloudDocs;

    public CloudDocSearchProcessorImpl(ElasticIndexStore indexStorage, GoogleCloudDocs cloudDocs) {
        this.indexStorage = indexStorage;
        this.cloudDocs = cloudDocs;
    }

    public void update() {
        for (Document document : cloudDocs.listDocument()) {
            var doc = indexStorage.getDocument(document.getId());
            if (!doc.found()) {
                var data = cloudDocs.getDocumentContent(document.getId());
                indexStorage.updateDocument(document, data);
                doc = indexStorage.getDocument(document.getId());
            }

            if (doc.found() && !ObjectUtils.equals(document.getVersion(), doc.getDocument().getVersion())
                    || document.isDeleted() != doc.getDocument().isDeleted()) {
                var data = cloudDocs.getDocumentContent(document.getId());
                indexStorage.updateDocument(document, data);
            }
        }
    }

    public List<String> search(String searchText) {
        var docs = indexStorage.searchDocuments(searchText);

        List<String> list = new ArrayList<>();
        for (String docId : docs) {
            if (!cloudDocs.isDocumentPresent(docId)) {
                indexStorage.deleteDocument(docId);
            } else {
                var doc = indexStorage.getDocument(docId);
                if (doc.found()) {
                    list.add(doc.getDocument().getPath());
                }
            }
        }
        return list;
    }
}
