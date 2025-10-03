package com.example.knot_server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("message_attachments")
public class MessageAttachment {

    @TableId(value = "att_id", type = IdType.AUTO)
    private Long attId;

    @TableField("msg_id")
    private Long msgId;

    private Integer kind; // 1=image,2=video,3=file

    private String url;

    @TableField("thumb_url")
    private String thumbUrl;

    // 元信息：{"w":...,"h":...,"durationMs":...,"size":...,"mime":"..."}
    @TableField("meta_json")
    private String metaJson;

    @TableField("sort_no")
    private Integer sortNo;    // 排序号，从0开始

}
