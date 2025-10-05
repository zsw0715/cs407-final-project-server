package com.example.knot_server.service.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageSavedView {
    private Long msgId;
    private Long convId;
    private Long fromUid;
    private Integer msgType;
    private String contentText;
    private String mediaUrl;
    private String mediaThumbUrl;
    private String mediaMetaJson;
    private Long createdAtMs;

    // 需要推送的目标用户（同会话成员）
    private List<Long> receiverUids;

    // --- 组装成 WS 报文 ---
    public Map<String, Object> toAck(String clientMsgId) {
        return Map.of(
                "type", "MSG_ACK",
                "clientMsgId", clientMsgId,
                "msgId", msgId,
                "serverTime", createdAtMs);
    }

    public Map<String, Object> toMsgNew() {
        return Map.of(
                "type", "MSG_NEW",
                "msgId", msgId,
                "convId", convId,
                "from", fromUid,
                "msgType", msgType,
                "contentText", contentText,
                "mediaUrl", mediaUrl,
                "mediaThumbUrl", mediaThumbUrl,
                "mediaMetaJson", mediaMetaJson,
                "createdAt", createdAtMs);
    }
}
