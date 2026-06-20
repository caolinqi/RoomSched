package org.example.roomsched;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能会议室预约系统 - 启动类
 */
@SpringBootApplication
@EnableScheduling // 启用定时任务（用于超时自动释放预约）
@MapperScan("org.example.roomsched.mapper") // 扫描 Mapper 接口
public class RoomSchedApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomSchedApplication.class, args);
    }

}
