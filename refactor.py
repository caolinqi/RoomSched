import os
import re

path = r'c:\Users\86155\Desktop\RoomSched\src\main\java\org\example\roomsched\service\impl\BookingServiceImpl.java'

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r'throw new RuntimeException\("会议室ID不能为空"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.ROOM_ID_NULL);'),
    (r'throw new RuntimeException\("会议主题不能为空"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TITLE_EMPTY);'),
    (r'throw new RuntimeException\("会议主题不能超过100字符"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TITLE_TOO_LONG);'),
    (r'throw new RuntimeException\("参会人数必须大于0"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_ATTENDEES_INVALID);'),
    (r'throw new RuntimeException\("开始时间和结束时间不能为空"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TIME_NULL);'),
    (r'throw new RuntimeException\("开始时间必须在结束时间之前"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TIME_INVALID);'),
    (r'throw new RuntimeException\("不能预约过去的时间"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_TIME_PAST);'),
    (r'throw new RuntimeException\("单次预约不能超过8小时"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_DURATION_EXCEED);'),
    (r'throw new RuntimeException\("必须提前30分钟预约"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_ADVANCE_TIME);'),
    (r'throw new RuntimeException\("会议室不存在"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.ROOM_NOT_FOUND);'),
    (r'throw new RuntimeException\("未找到当前登录用户预约"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.UNAUTHORIZED);'),
    (r'throw new RuntimeException\("参会人数\(" \+ record\.getAttendeeCount\(\) \+ "\)超过会议室容量\(" \+ room\.getCapacity\(\) \+ "\)"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_ATTENDEES_EXCEED, room.getCapacity());'),
    (r'throw new RuntimeException\("该时间段已被预约，请选择其他时间"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_CONFLICT);'),
    (r'throw new RuntimeException\("预约记录不存在"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.BOOKING_NOT_FOUND);'),
    (r'throw new RuntimeException\("只能取消待审批或已通过的预约"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_CANCEL_INVALID, record.getStatus().getDisplayName());'),
    (r'throw new RuntimeException\("当前状态为\[" \+ record\.getStatus\(\)\.getDisplayName\(\) \+ "\]，不可取消"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_CANCEL_INVALID, record.getStatus().getDisplayName());'),
    (r'throw new RuntimeException\("会议开始前30分钟内不可取消，请联系管理员"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_CANCEL_TIMEOUT);'),
    (r'throw new RuntimeException\("只能审批待审批的预约"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_APPROVE_INVALID, record.getStatus().getDisplayName());'),
    (r'throw new RuntimeException\("当前状态为\[" \+ record\.getStatus\(\)\.getDisplayName\(\) \+ "\]，不可审批"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_APPROVE_INVALID, record.getStatus().getDisplayName());'),
    (r'throw new RuntimeException\("不在签到时间范围内（会议开始前15分钟至开始后15分钟）"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_CHECKIN_TIME_INVALID);'),
    (r'throw new RuntimeException\("当前状态为\[" \+ record\.getStatus\(\)\.getDisplayName\(\) \+ "\]，不可签到"\);', r'throw new org.example.roomsched.exception.BusinessException(org.example.roomsched.exception.ErrorCode.STATUS_CHECKIN_INVALID, record.getStatus().getDisplayName());')
]

for old, new_ in replacements:
    content = re.sub(old, new_, content)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('BookingServiceImpl refactored!')
