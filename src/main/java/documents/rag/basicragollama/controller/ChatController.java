package documents.rag.basicragollama.controller;

import documents.rag.basicragollama.controller.request.ChatRequest;
import documents.rag.basicragollama.controller.response.ChatResponse;
import documents.rag.basicragollama.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/question")
    public ResponseEntity<ChatResponse> askQuestionAboutFile(@RequestBody @Valid ChatRequest chatRequest) {
        return ResponseEntity.ok(chatService.askQuestionAboutFile(chatRequest));
    }
}