package pl.planzy.scrappers.mapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface EventMapper {

    void mapEvents(List<JsonNode> data);

}
