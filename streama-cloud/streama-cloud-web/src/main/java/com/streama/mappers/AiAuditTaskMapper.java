package com.streama.mappers;

import com.streama.entity.po.AiAuditTask;
import org.apache.ibatis.annotations.Param;

public interface AiAuditTaskMapper {

    Integer insert(@Param("bean") AiAuditTask bean);

    Integer updateByTaskId(@Param("bean") AiAuditTask bean, @Param("taskId") Long taskId);

    AiAuditTask selectByRequestId(@Param("requestId") String requestId);

    AiAuditTask selectLatestByVideoId(@Param("videoId") String videoId);

    Integer selectMaxAuditVersionByVideoId(@Param("videoId") String videoId);
}

