package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import pl.planzy.scrappers.mapper.EventMapper;

import java.util.List;

public interface Scrapper {

    void scrapeData();
    List<JsonNode> getResults();
    EventMapper getMapper();

}
