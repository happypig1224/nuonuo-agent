package com.nuonuo.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuonuo.user.entity.UserAccountEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户基础信息 Mapper 接口。
 *
 * @author nuonuo-agent
 * @since 2026-04-27
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {
}
