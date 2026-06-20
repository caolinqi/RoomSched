package org.example.roomsched.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String username;
    
    private String action;
    
    private Long targetId;
    
    private String details;
    
    private String ipAddress;
    
    private LocalDateTime createTime;
}
