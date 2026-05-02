package com.streama.services;

import com.streama.entity.po.VideoDanmu;
import com.streama.entity.query.VideoDanmuQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.exception.BusinessException;

import java.util.List;

/**
 * @Description:视频弹幕Service
 * @author:孙将斌
 * @date:2026/03/11
 */
public interface VideoDanmuService{

	/**
	 * 根据条件查询列表
	 */
	List<VideoDanmu> findListByParam(VideoDanmuQuery query);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(VideoDanmuQuery query);

	/**
	 * 分页查询
	 */
	PaginationResultVO<VideoDanmu> findListByPage(VideoDanmuQuery query);

	/**
	 * 新增
	 */
	Integer add(VideoDanmu bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<VideoDanmu> listBean);

	/**
	 * 新增或更新
	 */
	Integer addOrUpdate(VideoDanmu bean);

	/**
	 * 批量新增或更新
	 */
	Integer addOrUpdateBatch(List<VideoDanmu> listBean);

	/**
	 * 根据DanmuId查询
	 */
	VideoDanmu getVideoDanmuByDanmuId(Integer danmuId);

	/**
	 * 根据DanmuId更新
	 */
	Integer updateVideoDanmuByDanmuId(VideoDanmu bean, Integer danmuId);

	/**
	 * 根据DanmuId删除
	 */
	Integer deleteVideoDanmuByDanmuId(Integer danmuId);

	Integer deleteByParam(VideoDanmuQuery query);

	void saveVideoDanmu(VideoDanmu videoDanmu) throws BusinessException;

    void deleteDanmu(String userId, Integer danmuId) throws BusinessException;
}
