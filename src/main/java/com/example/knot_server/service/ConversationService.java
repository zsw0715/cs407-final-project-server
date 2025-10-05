package com.example.knot_server.service;

public interface ConversationService {
    Long getOrCreateSingleConv(Long uidA, Long uidB);
}
