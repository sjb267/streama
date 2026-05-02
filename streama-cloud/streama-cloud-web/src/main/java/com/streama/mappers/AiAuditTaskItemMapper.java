package com.streama.mappers;

import com.streama.entity.po.AiAuditTaskItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiAuditTaskItemMapper {

    Integer insertBatch(@Param("list") List<AiAuditTaskItem> list);

    Integer updateByTaskIdAndFileId(@Param("bean") AiAuditTaskItem bean, @Param("taskId") Long taskId, @Param("fileId") String fileId);

    List<AiAuditTaskItem> selectByTaskId(@Param("taskId") Long taskId);
}

