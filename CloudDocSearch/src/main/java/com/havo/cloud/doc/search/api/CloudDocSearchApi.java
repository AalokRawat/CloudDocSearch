package com.havo.cloud.doc.search.api;

import com.havo.cloud.doc.search.service.CloudDocSearchProcessor;
import com.havo.cloud.doc.search.service.CloudDocSearchProcessorImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class CloudDocSearchApi {

    private final CloudDocSearchProcessor cloudDocSearchProcessor;

    public CloudDocSearchApi(CloudDocSearchProcessorImpl cloudDocSearchProcessor) {
        this.cloudDocSearchProcessor = cloudDocSearchProcessor;
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam(value = "q") String q) {
        try {
            return cloudDocSearchProcessor.search(q);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred while search for docs with query param " + q, e);
        }
    }
}
