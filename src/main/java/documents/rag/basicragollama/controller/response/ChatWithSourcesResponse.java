package documents.rag.basicragollama.controller.response;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record ChatWithSourcesResponse(

        String response,

        @JsonPropertyDescription("List of page references from the document used to answer, e.g. 'Page 3', 'Page 7'. If not available, return an empty list.")
        List<String> sources

) {
}

