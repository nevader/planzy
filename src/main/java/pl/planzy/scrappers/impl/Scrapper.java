package pl.planzy.scrappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import pl.planzy.scrappers.mapper.EventMapper;

import java.util.List;

public interface Scrapper {

    List<JsonNode> scrapeData();
    EventMapper getMapper();

}
