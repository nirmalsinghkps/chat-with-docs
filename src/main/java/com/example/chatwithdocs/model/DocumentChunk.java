package com.example.chatwithdocs.model;

public record DocumentChunk(String source, String text, float[] embedding) {
}
