package com.travel.flightdeals.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonParser() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String textOrEmpty(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? "" : child.asText();
    }
}
