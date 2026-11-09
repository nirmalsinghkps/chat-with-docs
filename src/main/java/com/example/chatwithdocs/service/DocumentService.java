package com.example.chatwithdocs.service;

import com.example.chatwithdocs.model.DocumentChunk;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DocumentService {
    private final OllamaService ollamaService;
    private final List<DocumentChunk> chunks = new CopyOnWriteArrayList<>();

    public DocumentService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public int upload(MultipartFile file) throws IOException {
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "document");
        String text;
        if (filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            try (var pdf = Loader.loadPDF(file.getBytes())) {
                text = new PDFTextStripper().getText(pdf);
            }
        } else if (filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            text = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Only PDF and TXT files are supported.");
        }

        List<String> pieces = splitText(text, 900, 150);
        if (pieces.isEmpty()) {
            throw new IllegalArgumentException("No readable text found in this file. Scanned PDFs need OCR.");
        }

        List<DocumentChunk> newChunks = new ArrayList<>();
        for (String piece : pieces) {
            newChunks.add(new DocumentChunk(filename, piece, ollamaService.createEmbedding(piece)));
        }
        chunks.addAll(newChunks);
        return newChunks.size();
    }

    public List<DocumentChunk> search(String question, int topK) {
        if (chunks.isEmpty()) {
            throw new IllegalStateException("Upload and process a document first.");
        }
        float[] queryVector = ollamaService.createEmbedding(question);
        return chunks.stream()
                .map(chunk -> Map.entry(chunk, cosineSimilarity(queryVector, chunk.embedding())))
                .sorted(Map.Entry.<DocumentChunk, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    public int chunkCount() {
        return chunks.size();
    }

    public void clear() {
        chunks.clear();
    }

    private List<String> splitText(String input, int size, int overlap) {
        String text = input.replaceAll("\\s+", " ").trim();
        List<String> result = new ArrayList<>();
        int step = size - overlap;
        for (int start = 0; start < text.length(); start += step) {
            String part = text.substring(start, Math.min(start + size, text.length())).trim();
            if (!part.isBlank()) result.add(part);
        }
        return result;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }
}
