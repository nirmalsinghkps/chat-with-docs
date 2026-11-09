
package com.example.chatwithdocs.controller;

import com.example.chatwithdocs.model.ChatRequest;
import com.example.chatwithdocs.model.ChatResponse;
import com.example.chatwithdocs.model.DocumentChunk;
import com.example.chatwithdocs.service.DocumentService;
import com.example.chatwithdocs.service.OllamaService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final OllamaService ollamaService;

    public DocumentController(
            DocumentService documentService,
            OllamaService ollamaService) {
        this.documentService = documentService;
        this.ollamaService = ollamaService;
    }

    @PostMapping("/documents")
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Document upload started ({} bytes).", file.getSize());
        int chunks = documentService.upload(file);
        log.info("Document upload completed: {} chunks indexed.", chunks);
        return ResponseEntity.ok("Document processed. Created " + chunks + " chunks.");
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {

        log.info("Chat request started.");
        List<DocumentChunk> matches =
                documentService.search(request.question(), 4);
        log.info("Retrieved {} document chunks for the answer.", matches.size());

        String context = matches.stream()
                .map(chunk ->
                        "[Source: " + chunk.source() + "] " + chunk.text())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        String answer =
                ollamaService.answer(request.question(), context);

        List<String> sources = matches.stream()
                .map(DocumentChunk::source)
                .distinct()
                .toList();

        log.info("Chat request completed using {} source file(s).", sources.size());
        return new ChatResponse(answer, sources);
    }

    @GetMapping("/status")
    public Status status() {
        return new Status(documentService.chunkCount());
    }

    @DeleteMapping("/documents")
    public ResponseEntity<String> clear() {
        documentService.clear();
        log.info("Indexed documents cleared.");
        return ResponseEntity.ok("All indexed documents cleared.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequest(IllegalArgumentException exception) {
        log.warn("Invalid document request: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(IllegalStateException exception) {
        log.warn("Document request could not be completed: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleOllamaResponse(RestClientResponseException exception) {
        String message = "Local AI service returned HTTP " + exception.getStatusCode().value()
                + ": " + exception.getResponseBodyAsString();
        log.warn("{}", message);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", message));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleOllamaConnection(ResourceAccessException exception) {
        String message = "Could not connect to Ollama. Verify it is running and accessible at the configured base URL.";
        log.warn("{} {}", message, exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", message));
    }

    public record Status(int indexedChunks) {}
}
