package org.example.roomsched.vo;

import lombok.Data;
import org.example.roomsched.entity.MeetingRoom;

import java.util.List;

/**
 * 会议室视图对象
 * 用于前端展示，包含解析后的设备列表
 */
@Data
public class RoomVO {

    /** 会议室ID */
    private Long id;

    /** 会议室名称 */
    private String roomName;

    /** 容纳人数 */
    private Integer capacity;

    /** 位置描述 */
    private String location;

    /** 设备配置（JSON字符串） */
    private String facilities;

    /** 设备列表（解析后） */
    private List<String> facilityList;

    /** 状态：0维修中 1可用 2停用 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /**
     * 从实体转换为 VO
     */
    public static RoomVO fromEntity(MeetingRoom room) {
        RoomVO vo = new RoomVO();
        vo.setId(room.getId());
        vo.setRoomName(room.getRoomName());
        vo.setCapacity(room.getCapacity());
        vo.setLocation(room.getLocation());
        vo.setFacilities(room.getFacilities());
        vo.setStatus(room.getStatus());

        // 状态文本转换
        if (room.getStatus() == 0) {
            vo.setStatusText("维修中");
        } else if (room.getStatus() == 1) {
            vo.setStatusText("可用");
        } else if (room.getStatus() == 2) {
            vo.setStatusText("停用");
        }

        // 解析设备 JSON（简单处理，实际可用 Jackson）
        if (room.getFacilities() != null && !room.getFacilities().isEmpty()) {
            // 移除 JSON 数组符号，按逗号分割
            String facilitiesStr = room.getFacilities()
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "");
            vo.setFacilityList(List.of(facilitiesStr.split(",")));
        }

        return vo;
    }
}
