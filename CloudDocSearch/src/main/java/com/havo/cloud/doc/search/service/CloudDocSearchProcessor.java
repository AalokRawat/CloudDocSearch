package com.havo.cloud.doc.search.service;

import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.google.api.services.drive.model.File;
import com.havo.cloud.doc.search.datastore.ElasticStore;
import com.havo.cloud.doc.search.datastore.GoogleCloudDocs;
import com.havo.cloud.doc.search.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CloudDocSearchProcessor {
    private final ElasticStore elasticStore;
    private final GoogleCloudDocs googleCloudDocs;

    public CloudDocSearchProcessor(ElasticStore elasticStore, GoogleCloudDocs googleCloudDocs) {
        this.elasticStore = elasticStore;
        this.googleCloudDocs = googleCloudDocs;
    }

    public void update() throws IOException, TikaException, SAXException {
        for (File file : googleCloudDocs.listFiles()) {
            GetResponse<Document> doc = elasticStore.getDocument(file.getId());
            if(!doc.found()) {
                var stream = googleCloudDocs.getFileContent(file.getId());
                elasticStore.updateDocument(file, stream);
                doc = elasticStore.getDocument(file.getId());
            }

            if(doc.source()!=null && !ObjectUtils.equals(file.getVersion(), doc.source().getVersion()) ||
                    file.getTrashed().booleanValue() != doc.source().isDeleted()) {
                var stream = googleCloudDocs.getFileContent(file.getId());
                elasticStore.updateDocument(file, stream);
            }
        }
    }

    public List<String> search(String searchText) throws IOException {
        SearchResponse<Document> response = elasticStore.searchDocuments(searchText);

        List<String> list = new ArrayList<>();
        for(Hit hit : response.hits().hits()) {

            if (!googleCloudDocs.isFilePresent(hit.id())) {
                elasticStore.deleteDocument(hit.id());
            } else {
                GetResponse<Document> doc = elasticStore.getDocument(hit.id());
                if(doc.found()) {
                    list.add(doc.source().getPath());
                }
            }
        }
        return list;
    }
}
