package com.example.knot_server.service;

import com.example.knot_server.netty.server.model.WsSendMessage;
import com.example.knot_server.service.dto.MessageSavedView;

public interface MessageService {
    MessageSavedView saveFromWebSocket(Long senderUid, WsSendMessage req);
}
