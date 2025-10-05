package com.example.knot_server.netty.server.handler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.knot_server.netty.server.model.WsSendMessage;
import com.example.knot_server.netty.session.ChannelAttrs;
import com.example.knot_server.netty.session.LocalSessionRegistry;
import com.example.knot_server.service.MessageService;
import com.example.knot_server.service.dto.MessageSavedView;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class MessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ObjectMapper om;
    private final MessageService messageService;
    private final LocalSessionRegistry registry;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String text = msg.text();

        WsSendMessage req;
        try {
            req = om.readValue(text, WsSendMessage.class);
        } catch (Exception parseEx) {
            // 不是 JSON / 或不是本 handler 关心的——放行给后续
            ctx.fireChannelRead(msg.retain());
            return;
        }
        if (req.getType() == null || !"MSG_SEND".equalsIgnoreCase(req.getType())) {
            ctx.fireChannelRead(msg.retain());
            return;
        }

        // 必须已 AUTH（在 AuthHandler 里把 UID 放到了 Channel Attr）
        Long uid = ctx.channel().attr(ChannelAttrs.UID).get();
        if (uid == null) {
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"not authed\"}"));
            return;
        }

        try {
            // 1) 业务入库（含成员校验/幂等/更新会话），返回保存结果 + 会话成员列表
            MessageSavedView saved = messageService.saveFromWebSocket(uid, req);

            // 2) 回 ACK 给发送者
            var ack = om.createObjectNode();
            ack.put("type", "MSG_ACK");
            ack.put("msgId", saved.getMsgId());
            ack.put("clientMsgId", req.getClientMsgId());
            ack.put("serverTime", System.currentTimeMillis());
            ctx.writeAndFlush(new TextWebSocketFrame(om.writeValueAsString(ack)));

            // 3) 组装下发 payload（发给其他会话成员）
            var push = om.createObjectNode();
            push.put("type", "MSG_NEW");
            push.put("convId", saved.getConvId());
            push.put("msgId", saved.getMsgId());
            push.put("fromUid", saved.getFromUid());
            push.put("msgType", saved.getMsgType());
            if (saved.getContentText() != null)   push.put("contentText", saved.getContentText());
            if (saved.getMediaUrl() != null)      push.put("mediaUrl", saved.getMediaUrl());
            if (saved.getMediaThumbUrl() != null) push.put("mediaThumbUrl", saved.getMediaThumbUrl());
            if (saved.getMediaMetaJson() != null) {
                try { push.set("mediaMeta", om.readTree(saved.getMediaMetaJson())); } catch (Exception ignore) {}
            }
            if (saved.getCreatedAtMs() != null)   push.put("createdAtMs", saved.getCreatedAtMs());
            String payload = om.writeValueAsString(push);

            // 4) 广播（逐个 try/catch，失败不影响其他人）
            List<Long> recipients = saved.getReceiverUids();
            if (recipients != null) {
                for (Long ruid : recipients) {
                    if (ruid == null || ruid.equals(uid)) continue; // 可选：不推给自己
                    try {
                        Channel ch = registry.byUid(ruid);
                        if (ch == null || !ch.isActive()) continue;   // 不在本机/离线，跳过
                        ch.writeAndFlush(new TextWebSocketFrame(payload));
                    } catch (Exception eOne) {
                        log.warn("[MSG] push to uid={} failed: {}", ruid, eOne.toString());
                    }
                }
            }
        } catch (IllegalArgumentException bad) {
            // 业务性错误（如：非会话成员、参数不全）
            var err = om.createObjectNode();
            err.put("type", "ERROR");
            err.put("msg", bad.getMessage() == null ? "bad request" : bad.getMessage());
            ctx.writeAndFlush(new TextWebSocketFrame(err.toString()));
        } catch (Exception e) {
            log.error("MSG_SEND failed", e);
            ctx.writeAndFlush(new TextWebSocketFrame("{\"type\":\"ERROR\",\"msg\":\"server error\"}"));
        }
    }
}