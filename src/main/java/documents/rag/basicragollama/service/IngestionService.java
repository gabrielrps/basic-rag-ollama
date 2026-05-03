package documents.rag.basicragollama.service;

import documents.rag.basicragollama.entity.DocumentFile;
import documents.rag.basicragollama.exception.IngestionException;
import documents.rag.basicragollama.repository.DocumentFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class IngestionService {

    private final VectorStore vectorStore;
    private final DocumentFileRepository documentFileRepository;

    @Transactional
    public void ingestDocument(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename();

            String fileHash = calculateHash(fileBytes);
            deleteVectorIfExists(fileHash);

            Resource resource = new ByteArrayResource(fileBytes);

            List<Document> documents = getDocuments(file, resource);

            TokenTextSplitter splitter = getTokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(documents);

            splitDocuments = sanitizedDocs(splitDocuments, fileHash);

            vectorStore.accept(splitDocuments);
            saveOrUpdate(fileName, fileHash);
        } catch (IOException e) {
            throw new IngestionException(String.format("Error to read the document: %s", file.getOriginalFilename()), e);
        }
    }

    public void saveOrUpdate(String fileName, String fileHash) {
        DocumentFile file = documentFileRepository.findByFileHash(fileHash).orElseGet(DocumentFile::new);

        file.setFileName(fileName);
        file.setFileHash(fileHash);

        documentFileRepository.save(file);
    }

    private void deleteVectorIfExists(String fileHash) {
        var filterExpression = new FilterExpressionBuilder().eq("file_hash", fileHash).build();
        vectorStore.delete(filterExpression.toString());
    }

    private List<Document> sanitizedDocs(List<Document> splitDocuments, String fileHash) {
        return splitDocuments.stream()
                .map(doc -> {
                    String originalText = doc.getText();
                    if (originalText == null || originalText.isEmpty()) return doc;

                    String cleanContent = originalText
                            .replace("\u0000", "")
                            .replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");

                    doc.getMetadata().put("file_hash", fileHash);

                    return new Document(cleanContent, doc.getMetadata());
                })
                .toList();
    }

    private static List<Document> getDocuments(MultipartFile file, Resource resource) {
        List<Document> documents;

        if (Objects.requireNonNull(file.getOriginalFilename()).endsWith(".pdf")) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            documents = pdfReader.get();
        } else {
            TextReader textReader = new TextReader(resource);
            documents = textReader.get();
        }

        if (documents.isEmpty()) {
            throw new IngestionException(String.format("Error to convert the file: %s", file.getOriginalFilename()), null);
        }
        return documents;
    }

    private TokenTextSplitter getTokenTextSplitter() {
        return new TokenTextSplitter(
                200,
                50,
                5,
                1000,
                true,
                Arrays.asList('.', '!', '?', '\n')
        );
    }

    private String calculateHash(byte[] fileBytes){
        return DigestUtils.md5DigestAsHex(fileBytes);
    }
}
