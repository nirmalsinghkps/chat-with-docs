package com.example.chatwithdocs.model;

import java.util.List;

public record ChatResponse(String answer, List<String> sources) {
}
