package org.example.roomsched.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.roomsched.entity.MeetingRoom;

import java.util.List;

/**
 * 会议室 Service 接口
 */
public interface RoomService extends IService<MeetingRoom> {

    /**
     * 查询所有可用会议室
     * 
     * @return 会议室列表
     */
    List<MeetingRoom> listAvailableRooms();

    /**
     * 根据 ID 查询会议室
     * 
     * @param id 会议室ID
     * @return 会议室实体
     */
    MeetingRoom getRoomById(Long id);

    /**
     * 新增会议室
     * 
     * @param room 会议室实体
     */
    void addRoom(MeetingRoom room);

    /**
     * 更新会议室
     * 
     * @param room 会议室实体
     */
    void updateRoom(MeetingRoom room);

    /**
     * 删除会议室
     * 
     * @param id 会议室ID
     */
    void deleteRoom(Long id);
}
