package com.havo.cloud.doc.search.datastore.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.havo.cloud.doc.search.exception.CloudDocSearchException;
import com.havo.cloud.doc.search.model.Document;
import com.havo.cloud.doc.search.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ElasticIndexStore implements IndexStorage {
    public static final String AUTHORIZATION = "Authorization";
    public static final String API_KEY = "ApiKey ";
    private final ElasticConfigProperties elasticConfigProperties;
    private ElasticsearchClient esClient;

    @Autowired
    public ElasticIndexStore(ElasticConfigProperties elasticConfigProperties) {
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


    public Response getDocument(String id) {
        co.elastic.clients.elasticsearch.core.GetResponse<Document> response;
        try {
            response = esClient.get(g -> g.index(elasticConfigProperties.getIndex()).id(id), Document.class);
        } catch (IOException e) {
            String msg = "Failed to get the document with id " + id + " from storage index.";
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
        return new Response(response.source(), response.found());
    }

    public void updateDocument(Document document, String data) {
        document.setText(data);
        try {
            esClient.index(i -> i.index(elasticConfigProperties.getIndex()).id(document.getId()).document(document));
        } catch (IOException e) {
            String msg = "Failed to update the document with id " + document.getId() + " in storage index.";
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
    }

    public List<String> searchDocuments(String searchText) {
        Query byText = MatchQuery.of(m -> m
                .field("text")
                .query(searchText)
        )._toQuery();

        // Search by max price
        Query byNotDeleted = MatchQuery.of(r -> r
                .field("deleted")
                .query(false)
        )._toQuery();

        try {
            return esClient.search(s -> s
                            .index(elasticConfigProperties.getIndex())
                            .query(q -> q.bool(b -> b.must(byText).must(byNotDeleted))),
                    Document.class).hits().hits().parallelStream().map(Hit::id).collect(Collectors.toList());
        } catch (IOException e) {
            String msg = "Failure while searching the text " + searchText;
            log.error(msg);
            throw new CloudDocSearchException(msg, e);
        }
    }

    public void deleteDocument(String documentId) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d.index(elasticConfigProperties.getIndex()).id(documentId));
            esClient.delete(request);
        } catch (IOException e) {
            log.error("Failed to delete the document {}", documentId);
        }
    }
}
