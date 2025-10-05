package com.example.knot_server.netty.server.model;

import lombok.Data;

@Data
public class WsSendMessage {
    private String type = "MSG_SEND";
    private Long convId;
    private String clientMsgId; 
    private Integer msgType;
    private String contentText;
    private String mediaUrl;
    private String mediaThumbUrl;
    private String mediaMetaJson;
    private Long replyToMsgId;

    private Loc loc;
    @Data public static class Loc {
        private Double lat;
        private Double lng;
        private String name;
        private Float accuracy;
    }
}