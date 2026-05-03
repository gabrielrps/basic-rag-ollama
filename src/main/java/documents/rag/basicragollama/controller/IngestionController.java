package documents.rag.basicragollama.controller;

import documents.rag.basicragollama.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> ingestDocument(@RequestParam("file") MultipartFile file) {

        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.toLowerCase().endsWith(".pdf") || fileName.toLowerCase().endsWith(".md"))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ingestionService.ingestDocument(file);

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }
}