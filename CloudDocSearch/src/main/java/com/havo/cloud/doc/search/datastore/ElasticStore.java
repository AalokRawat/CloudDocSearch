package com.havo.cloud.doc.search.datastore;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.google.api.services.drive.model.File;
import com.havo.cloud.doc.search.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class ElasticStore {
    public static final String AUTHORIZATION = "Authorization";
    public static final String API_KEY = "ApiKey ";
    private ElasticConfigProperties elasticConfigProperties;
    private ElasticsearchClient esClient;

    @Autowired
    public ElasticStore(ElasticConfigProperties elasticConfigProperties) {
        this.elasticConfigProperties = elasticConfigProperties;
        init();
    }

    private void init() {
        RestClient restClient = RestClient
                .builder(HttpHost.create(elasticConfigProperties.getServerUrl()))
                .setDefaultHeaders(new BasicHeader[]{
                        new BasicHeader(AUTHORIZATION, API_KEY + elasticConfigProperties.getApiKey())
                })
                .build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);
    }

    public GetResponse<Document> getDocument(String id) throws IOException {
        return esClient.get(g -> g
                        .index(elasticConfigProperties.getIndex())
                        .id(id),
                Document.class
        );
    }

    public void updateDocument(File file, InputStream stream) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();
        parser.parse(stream, handler, metadata, new ParseContext());

        Document document = new Document();
        document.setName(file.getName());
        document.setVersion(file.getVersion());
        document.setDeleted(file.getTrashed());
        document.setPath(file.getWebViewLink());
        document.setText(handler.toString());
        document.setType(getDocType(file.getMimeType()));

        esClient.index(i -> i.index(elasticConfigProperties.getIndex()).id(file.getId()).document(document));
    }

    public SearchResponse<Document> searchDocuments(String searchText) throws IOException {

        Query byText = MatchQuery.of(m -> m
                .field("text")
                .query(searchText)
        )._toQuery();

        // Search by max price
        Query byNotDeleted = MatchQuery.of(r -> r
                .field("deleted")
                .query(false)
        )._toQuery();

        return esClient.search(s -> s
                        .index(elasticConfigProperties.getIndex())
                        .query(q -> q.bool(b -> b.must(byText).must(byNotDeleted))),
                Document.class);
    }

    public void deleteDocument(String id) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d.index(elasticConfigProperties.getIndex()).id(id));
            esClient.delete(request);
        } catch (IOException e) {
            log.error("Failed to delete the document {}", id);
        }
    }

    private String getDocType(String mimeType) {
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
