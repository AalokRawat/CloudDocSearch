package com.havo.cloud.doc.search.datastore.cloud.docs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gcp.docs")
public class GoogleCloudDocsConfigProperties {
    private String userId;
    private String secretPath;
    private List<String> supportedFiles;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSecretPath() {
        return secretPath;
    }

    public void setSecretPath(String secretPath) {
        this.secretPath = secretPath;
    }

    public List<String> getSupportedFiles() {
        return supportedFiles;
    }

    public void setSupportedFiles(List<String> supportedFiles) {
        this.supportedFiles = supportedFiles;
    }
}
