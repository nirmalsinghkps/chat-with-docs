package com.example.chatwithdocs.model;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String question) {
}
