package com.example.knot_server.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long userid;
    
    @TableField("username")
    private String username;
    
    @TableField("password_hash")
    private String passwordHash;
    
    @TableField("nickname")
    private String nickname;
    
    @TableField("email")
    private String email;
    
    @TableField("gender")
    private String gender;
    
    @TableField("age")
    private Integer age;
    
    @TableField("birthdate")
    private LocalDate birthdate;
    
    @TableField("avatar_url")
    private String avatarUrl;
    
    @TableField("account_status")
    private String accountStatus;
    
    @TableField("last_latitude")
    private BigDecimal lastLatitude;
    
    @TableField("last_longitude")
    private BigDecimal lastLongitude;
    
    @TableField("last_location_update")
    private LocalDateTime lastLocationUpdate;
    
    @TableField("status_message")
    private String statusMessage;
    
    @TableField("last_online_time")
    private LocalDateTime lastOnlineTime;
    
    @TableField("discoverable")
    private Boolean discoverable;
    
    @TableField("privacy_level")
    private String privacyLevel;
    
    @TableField("device_id")
    private String deviceId;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
