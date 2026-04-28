package com.nuonuo.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuonuo.chat.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {

    @Select("SELECT user_id AS userId, session_id AS sessionId " +
            "FROM chat_message " +
            "GROUP BY user_id, session_id " +
            "HAVING MAX(gmt_modified) < #{cutoff} " +
            "ORDER BY MAX(gmt_modified) ASC " +
            "LIMIT 100")
    List<Map<String, Object>> findExpiredSessions(@Param("cutoff") LocalDateTime cutoff);
}
