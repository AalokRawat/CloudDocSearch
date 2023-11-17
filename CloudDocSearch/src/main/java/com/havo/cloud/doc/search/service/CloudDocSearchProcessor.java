package com.havo.cloud.doc.search.service;

import java.util.List;

public interface CloudDocSearchProcessor {

    void update();

    List<String> search(String searchText);
}
