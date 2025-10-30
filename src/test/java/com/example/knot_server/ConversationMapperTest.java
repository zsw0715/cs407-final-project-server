package com.example.knot_server;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.knot_server.controller.dto.ConversationSummaryResponse;
import com.example.knot_server.mapper.ConversationMapper;

@SpringBootTest
public class ConversationMapperTest {

    @Autowired
    private ConversationMapper conversationMapper;

    @Test
    void testListUserConversations() {
        List<ConversationSummaryResponse> list = conversationMapper.listUserConversations(5L);
        list.forEach(System.out::println);
    }
}
