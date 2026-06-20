package org.example.roomsched.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.roomsched.entity.SysUser;

/**
 * 用户 Mapper 接口
 * 继承 MyBatis-Plus BaseMapper，自动获得基础 CRUD 方法
 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
