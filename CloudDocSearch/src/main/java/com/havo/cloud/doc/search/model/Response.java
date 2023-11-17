package com.havo.cloud.doc.search.model;

public class Response {
    Document document;
    boolean found;

    public Response(Document document, boolean found) {
        this.document = document;
        this.found = found;
    }

    public Document getDocument() {
        return document;
    }

    public boolean found() {
        return found;
    }
}
