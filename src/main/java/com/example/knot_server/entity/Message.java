package com.example.knot_server.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("messages")
public class Message {

    @TableId(value = "msg_id", type = IdType.AUTO)
    private Long msgId;

    @TableField("conv_id")
    private Long convId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("msg_type")
    private Integer msgType; // 0=TEXT,1=IMAGE,2=VIDEO,3=LOCATION,4=STICKER,5=SYSTEM

    @TableField("client_msg_id")
    private String clientMsgId;

    // 内容，TEXT=文本内容, LOCATION=经纬度JSON, STICKER=stickerId, SYSTEM=系统消息文本
    @TableField("content_text")
    private String contentText;

    // location info
    @TableField("loc_lat")
    private Double locLat;

    @TableField("loc_lng")
    private Double locLng;

    @TableField("loc_name")
    private String locName;

    @TableField("loc_accuracy_m")
    private Float locAccuracyM;

    private String geohash;

    // other message info
    @TableField("reply_to_msg_id")
    private Long replyToMsgId;

    @TableField("media_url")
    private String mediaUrl;

    @TableField("media_thumb_url")
    private String mediaThumbUrl;

    // JSON 列，简单起见用 String；需要的话再自定义 TypeHandler
    @TableField("media_meta_json")
    private String mediaMetaJson;

    @TableField("msg_status")
    private Integer msgStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("edited_at")
    private LocalDateTime editedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

}
