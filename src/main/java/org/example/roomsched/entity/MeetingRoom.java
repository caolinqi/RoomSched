package org.example.roomsched.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会议室实体类
 * 对应数据库表：meeting_room
 */
@Data
@TableName("meeting_room")
public class MeetingRoom {

    /** 会议室ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会议室名称 */
    private String roomName;

    /** 容纳人数 */
    private Integer capacity;

    /** 位置描述 */
    private String location;

    /** 设备配置，JSON格式如["投影仪","白板","视频会议"] */
    private String facilities;

    /** 状态：0维修中 1可用 2停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
