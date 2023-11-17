package com.havo.cloud.doc.search.exception;

public class CloudDocSearchException extends RuntimeException {

    public CloudDocSearchException(String message) {
        super(message);
    }

    public CloudDocSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
