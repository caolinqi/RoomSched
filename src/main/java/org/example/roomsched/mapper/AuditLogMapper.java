package org.example.roomsched.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.roomsched.entity.SysAuditLog;

@Mapper
public interface AuditLogMapper extends BaseMapper<SysAuditLog> {
}
