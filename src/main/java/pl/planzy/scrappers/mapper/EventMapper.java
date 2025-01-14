package pl.planzy.scrappers.mapper;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.List;

public interface EventMapper {

    List<JsonNode> mapEvents(List<JsonNode> data);

}
