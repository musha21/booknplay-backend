package lk.booknplay.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Accepts a JSON string or array and normalizes to a comma-separated string. */
public class CommaSeparatedOrListDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            node.forEach(item -> {
                if (item != null && !item.isNull()) {
                    String value = item.asText("").trim();
                    if (!value.isEmpty()) {
                        parts.add(value);
                    }
                }
            });
            return parts.stream().collect(Collectors.joining(","));
        }
        String text = node.asText(null);
        return text == null ? null : text.trim();
    }
}
