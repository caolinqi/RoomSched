package org.example.roomsched.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.entity.SysAuditLog;
import org.example.roomsched.mapper.AuditLogMapper;
import org.example.roomsched.util.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping("/page")
    public String auditLogPage() {
        return "admin/audit-log";
    }

    @GetMapping("/list")
    @ResponseBody
    public Result<Page<SysAuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer limit) {
        
        Page<SysAuditLog> logPage = new Page<>(page, limit);
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysAuditLog::getCreateTime);
        
        auditLogMapper.selectPage(logPage, wrapper);
        
        return Result.ok(logPage);
    }
}
