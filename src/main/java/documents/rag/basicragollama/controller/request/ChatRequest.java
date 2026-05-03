package documents.rag.basicragollama.controller.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ChatRequest {

    @NotNull
    private String question;

    @NotNull
    private String fileName;

}
