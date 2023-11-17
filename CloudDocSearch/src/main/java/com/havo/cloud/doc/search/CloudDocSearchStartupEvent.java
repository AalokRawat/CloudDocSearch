package com.havo.cloud.doc.search;

import com.havo.cloud.doc.search.service.CloudDocSearchProcessor;
import com.havo.cloud.doc.search.service.CloudDocSearchProcessorImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CloudDocSearchStartupEvent {

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 30;
    private final CloudDocSearchProcessor searchProcessor;

    @Autowired
    public CloudDocSearchStartupEvent(CloudDocSearchProcessorImpl searchProcessor) {
        this.searchProcessor = searchProcessor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Updating the search index.");
                searchProcessor.update();
            } catch (Exception e) {
                log.warn("Failed to update the search index", e);
            }
        }, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
    }

}
