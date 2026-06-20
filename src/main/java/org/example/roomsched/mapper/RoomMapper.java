package org.example.roomsched.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.roomsched.entity.MeetingRoom;

/**
 * 会议室 Mapper 接口
 */
@Mapper
public interface RoomMapper extends BaseMapper<MeetingRoom> {

    /**
     * 查询会议室并加行锁（SELECT ... FOR UPDATE）。
     * <p>
     * 在 submitBooking 事务内调用，用于序列化同一会议室的并发预约请求：
     * 两个并发事务会在此方法上排队，第一个完成冲突检测+插入后，第二个才执行，此时冲突已存在。
     * </p>
     *
     * @param id 会议室ID
     * @return 会议室对象（不为 null，因为上层已校验存在性）
     */
    @Select("SELECT * FROM meeting_room WHERE id = #{id} FOR UPDATE")
    MeetingRoom selectByIdForUpdate(@Param("id") Long id);
}
