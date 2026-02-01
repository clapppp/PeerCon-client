package project;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RawRequest {
    private final Map<String, String> body;

    public RawRequest(String request) {
        if (request == null || request.isEmpty()) {
            this.body = Collections.emptyMap();
            return;
        }

        String[] parts = request.split("\r\n\r\n", 2);
        String bodyPart = parts[1];

        body = stringToMap(bodyPart); //map.toString 하면 문자열이 어떻게 나오나 {key1=value1, }
    }

    public Map<String, String> getBody() {
        return body;
    }

    public static String makeRaw(Map<String, String> body) {
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : body.entrySet()) {
            raw.append(entry.getKey()).append(":").append(entry.getValue()).append("\r\n");
        }
        return "Content-Length:" + raw.toString().getBytes(StandardCharsets.UTF_8).length + "\r\n\r\n" + raw;
    }

    private Map<String, String> stringToMap(String body) {
        if (body == null || body.isEmpty()) {
            return Collections.emptyMap();
        }
        String[] lines = body.split("\r\n");
        Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            String[] part = line.split(":", 2);
            map.put(part[0].trim(), part[1].trim());
        }
        return map;
    }

    public Map<String, String> toStringToMap(String toString) {
        String content = toString.substring(1, toString.length() - 1);

        return Arrays.stream(content.split(", "))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> parts[0].trim(),
                        parts -> parts[1].trim()
                ));
    }
}
