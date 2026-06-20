package org.example.roomsched.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.roomsched.entity.MeetingRoom;
import org.example.roomsched.mapper.RoomMapper;
import org.example.roomsched.service.RoomService;
import org.example.roomsched.annotation.LogAction;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会议室 Service 实现类
 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, MeetingRoom> implements RoomService {

    @Override
    public List<MeetingRoom> listAvailableRooms() {
        return list(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, 1) // 状态为可用
                        .orderByAsc(MeetingRoom::getRoomName));
    }

    @Override
    public MeetingRoom getRoomById(Long id) {
        return getById(id);
    }

    @Override
    @LogAction("添加会议室")
    public void addRoom(MeetingRoom room) {
        save(room);
    }

    @Override
    @LogAction("更新会议室信息")
    public void updateRoom(MeetingRoom room) {
        updateById(room);
    }

    @Override
    @LogAction("删除会议室")
    public void deleteRoom(Long id) {
        removeById(id);
    }
}
