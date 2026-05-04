package documents.rag.basicragollama.service;

import documents.rag.basicragollama.controller.request.ChatRequest;
import documents.rag.basicragollama.controller.response.ChatResponse;
import documents.rag.basicragollama.controller.response.ChatWithSourcesResponse;
import documents.rag.basicragollama.entity.DocumentFile;
import documents.rag.basicragollama.exception.DocumentFileException;
import documents.rag.basicragollama.repository.DocumentFileRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final DocumentFileRepository documentFileRepository;

    public ChatService(ChatClient.Builder builder, VectorStore vectorStore, DocumentFileRepository documentFileRepository) {
        this.chatClient = builder
                .defaultSystem("""
                        You are a helpful assistant and an expert on the content of the documents provided.
                        Never repeat or rephrase the question in your response. Answer directly.
                        IMPORTANT: Always respond in the same language the user used to ask the question, even if the document context is in a different language
                        """)
                .build();
        this.vectorStore = vectorStore;
        this.documentFileRepository = documentFileRepository;
    }

    public ChatResponse askQuestionAboutFile(ChatRequest chatRequest) {
        var expression = getExpression(chatRequest);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(chatRequest.getQuestion())
                .topK(15)
                .filterExpression(expression)
                .similarityThreshold(0.5)
                .build();

        var advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        return ChatResponse.builder().response(chatClient.prompt()
                .user(chatRequest.getQuestion())
                .advisors(advisor)
                .call()
                .content()).build();
    }

    public ChatWithSourcesResponse askQuestionAboutFileWithSources(ChatRequest chatRequest) {
        var expression = getExpression(chatRequest);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(chatRequest.getQuestion())
                .topK(15)
                .filterExpression(expression)
                .similarityThreshold(0.5)
                .build();

        List<Document> docs = vectorStore.similaritySearch(searchRequest);

        String context = docs.stream()
                .map(doc -> {
                    Object page = doc.getMetadata().get("page_number");
                    String prefix = page != null ? "[Page " + page + "]\n" : "";
                    return prefix + doc.getText();
                })
                .collect(Collectors.joining("\n\n"));


        return chatClient.prompt()
                .user(u -> u.text("Context:\n{context}\n\nQuestion: {question}")
                        .param("context", context)
                        .param("question", chatRequest.getQuestion()))
                .call()
                .entity(ChatWithSourcesResponse.class);
    }

    private Filter.Expression getExpression(ChatRequest chatRequest) {
        String fileHash = getFileHash(chatRequest.getFileName());

        var filterBuilder = new FilterExpressionBuilder();
        return filterBuilder.eq("file_hash", fileHash).build();
    }

    private String getFileHash(String fileName) {
        DocumentFile documentFile = documentFileRepository.findByFileName(fileName).orElseThrow(() ->
                new DocumentFileException(String.format("Document File %s Not Found", fileName)));

        return documentFile.getFileHash();
    }


}
