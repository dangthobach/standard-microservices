package com.enterprise.business.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Compute field-level diff giữa 2 JSON snapshots
 * Dùng cho audit trail showing exactly what changed
 */
@Component
public class JsonDiffUtil {

    private final ObjectMapper objectMapper;

    public JsonDiffUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * So sánh 2 object, trả về JSON chứa các field thay đổi
     * Format: {"fieldName": {"from": oldValue, "to": newValue}}
     */
    public String computeDiff(Object previous, Object current) {
        if (previous == null && current == null)
            return null;
        if (previous == null)
            return null;
        if (current == null)
            return null;

        try {
            JsonNode prevNode = objectMapper.valueToTree(previous);
            JsonNode currNode = objectMapper.valueToTree(current);
            return calculateDiff(prevNode, currNode);
        } catch (Exception e) {
            return null;
        }
    }

    public String computeDiffJsonString(String previousJson, String currentJson) {
        if (previousJson == null && currentJson == null)
            return null;
        if (previousJson == null)
            return null;
        if (currentJson == null)
            return null;

        try {
            JsonNode prevNode = objectMapper.readTree(previousJson);
            JsonNode currNode = objectMapper.readTree(currentJson);
            return calculateDiff(prevNode, currNode);
        } catch (Exception e) {
            return null;
        }
    }

    private String calculateDiff(JsonNode prevNode, JsonNode currNode) throws Exception {
        ObjectNode diff = objectMapper.createObjectNode();

        prevNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode prevVal = entry.getValue();
            JsonNode currVal = currNode.get(key);

            if (!prevVal.equals(currVal)) {
                ObjectNode change = objectMapper.createObjectNode();
                change.set("from", prevVal);
                change.set("to", currVal != null ? currVal : NullNode.getInstance());
                diff.set(key, change);
            }
        });

        currNode.fields().forEachRemaining(entry -> {
            if (!prevNode.has(entry.getKey())) {
                ObjectNode change = objectMapper.createObjectNode();
                change.set("from", NullNode.getInstance());
                change.set("to", entry.getValue());
                diff.set(entry.getKey(), change);
            }
        });

        return diff.isEmpty() ? null : objectMapper.writeValueAsString(diff);
    }
}
