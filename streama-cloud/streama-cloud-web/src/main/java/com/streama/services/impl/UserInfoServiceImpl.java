package com.streama.services.impl;

import com.streama.component.RedisComponent;
import com.streama.entity.constants.Constants;
import com.streama.entity.dto.CountInfoDto;
import com.streama.entity.dto.SysSettingDto;
import com.streama.entity.dto.TokenUserInfoDto;
import com.streama.entity.dto.UserCountInfoDto;
import com.streama.entity.po.UserFocus;
import com.streama.entity.po.UserInfo;
import com.streama.entity.query.SimplePage;
import com.streama.entity.query.UserFocusQuery;
import com.streama.entity.query.UserInfoQuery;
import com.streama.entity.vo.PaginationResultVO;
import com.streama.entity.enums.PageSize;
import com.streama.entity.enums.ResponseCodeEnum;
import com.streama.entity.enums.UserSexEnum;
import com.streama.entity.enums.UserStatusEnum;
import com.streama.exception.BusinessException;
import com.streama.mappers.UserFocusMapper;
import com.streama.mappers.UserInfoMapper;
import com.streama.mappers.VideoInfoMapper;
import com.streama.redis.RedisConfig;
import com.streama.services.UserInfoService;
import com.streama.utils.CopyTools;
import com.streama.utils.StringTools;
import jakarta.annotation.Resource;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description:用户信息
Service
 * @author:孙将斌
 * @date:2026/01/17
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService{

	@Resource
	private UserInfoMapper<UserInfo,UserInfoQuery> userInfoMapper;

	@Resource
	private RedisComponent redisComponent;

    @Resource
    private RedisConfig redisConfig;

    @Resource
    private UserFocusMapper<UserFocus, UserFocusQuery> userFocusMapper;

    @Resource
    private VideoInfoMapper videoInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	public List<UserInfo> findListByParam(UserInfoQuery query) {
		return userInfoMapper.selectList(query);
	}

	/**
	 * 根据条件查询数量
	 */
	public Integer findCountByParam(UserInfoQuery query) {
		return userInfoMapper.selectCount(query);
	}

	/**
	 * 分页查询
	 */
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query) {
		Integer count = findCountByParam(query);
		Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserInfo> list = findListByParam(query);
		PaginationResultVO<UserInfo> result = new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	public Integer add(UserInfo bean) {
		return userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 新增或更新
	 */
	public Integer addOrUpdate(UserInfo bean) {
		return userInfoMapper.insertOrUpdate(bean);
	}

	/**
	 * 批量新增或更新
	 */
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 根据UserId查询
	 */
	public UserInfo getUserInfoByUserId(String userId) {
		return userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId更新
	 */
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	public Integer deleteUserInfoByUserId(String userId) {
		return userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email查询
	 */
	public UserInfo getUserInfoByEmail(String email) {
		return userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email更新
	 */
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	public Integer deleteUserInfoByEmail(String email) {
		return userInfoMapper.deleteByEmail(email);
	}

	/**
	 * 根据NickName查询
	 */
	public UserInfo getUserInfoByNickName(String nickName) {
		return userInfoMapper.selectByNickName(nickName);
	}

	/**
	 * 根据NickName更新
	 */
	public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
		return userInfoMapper.updateByNickName(bean, nickName);
	}

	/**
	 * 根据NickName删除
	 */
	public Integer deleteUserInfoByNickName(String nickName) {
		return userInfoMapper.deleteByNickName(nickName);
	}


	public void register(String email, String nickName, String password) throws BusinessException{
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if(userInfo != null) {
			throw new BusinessException("邮箱已经存在");
		}
		UserInfo nickNameUser = userInfoMapper.selectByNickName(nickName);
		if(nickNameUser != null) {
			throw new BusinessException("昵称已经存在");
		}

		userInfo = new UserInfo();
		String userId = StringTools.getRandomNumber(Constants.LENGTH_10);
		userInfo.setUserId(userId);
		userInfo.setEmail(email);
		userInfo.setNickName(nickName);
		userInfo.setPassword(StringTools.encodeByMd5(password));
		userInfo.setJoinTime(new Date());
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		userInfo.setSex(UserSexEnum.SECRECY.getType());

		SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
		userInfo.setTotalCoinCount(sysSettingDto.getRegisterCoinCount());
		userInfo.setCurrentCoinCount(sysSettingDto.getRegisterCoinCount());

		userInfoMapper.insert(userInfo);
	}

	@Override
	public TokenUserInfoDto login(String email, String password, String ip) throws BusinessException {
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if(userInfo == null || !userInfo.getPassword().equals(StringTools.encodeByMd5(password))) {
			throw new BusinessException("账号或者密码不正确");
		}
		if(UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
			throw new BusinessException("账号已禁用");
		}
		UserInfo updateInfo = new UserInfo();
		updateInfo.setLastLoginIp(ip);
		updateInfo.setLastLoginTime(new Date());
		userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());

		TokenUserInfoDto tokenUserInfoDto = CopyTools.copy(userInfo, TokenUserInfoDto.class);
		redisComponent.saveTokenInfo(tokenUserInfoDto);
		return tokenUserInfoDto;
	}

	@Override
	public UserInfo getUserDetailInfo(String currentUserId, String userId) throws BusinessException {
		UserInfo userInfo = getUserInfoByUserId(userId);

		if(userInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		// 获赞数，播放数
		CountInfoDto countInfoDto = videoInfoMapper.selectSumCountInfo(userId);
		CopyTools.copyProperties(countInfoDto, userInfo);

		Integer fansCount = userFocusMapper.selectFansCount(userId);
		Integer focusCount = userFocusMapper.selectFocusCount(userId);
		userInfo.setFansCount(fansCount);
		userInfo.setFocusCount(focusCount);

		if(currentUserId == null) {
			userInfo.setHaveFocus(false);
		} else {
			UserFocus userFocus = userFocusMapper.selectByUserIdAndFocusUserId(currentUserId, userId);
			userInfo.setHaveFocus(userFocus != null);
		}

		return userInfo;
	}

	@Override
	@GlobalTransactional
	public void updateUserInfo(UserInfo userInfo, TokenUserInfoDto tokenUserInfoDto) throws BusinessException {
		userInfoMapper.updateByUserId(userInfo, tokenUserInfoDto.getUserId());
		Boolean updateTokenInfo = false;
		if (!userInfo.getAvatar().equals(tokenUserInfoDto.getAvatar())) {
			updateTokenInfo = true;
			tokenUserInfoDto.setAvatar(userInfo.getAvatar());
		}

		if (!userInfo.getNickName().equals(tokenUserInfoDto.getNickName())) {
			updateTokenInfo = true;
			tokenUserInfoDto.setNickName(userInfo.getNickName());
		}

		if(updateTokenInfo) {
			redisComponent.updateTokenInfo(tokenUserInfoDto);
		}
	}

	@Override
	public UserCountInfoDto getUserCountInfo(String userId) throws BusinessException {
		UserInfo userInfo = getUserInfoByUserId(userId);
		Integer fansCount = userFocusMapper.selectFansCount(userId);
		Integer focusCount = userFocusMapper.selectFocusCount(userId);
		UserCountInfoDto userCountInfoDto = new UserCountInfoDto();
		userCountInfoDto.setFansCount(fansCount);
		userCountInfoDto.setFocusCount(focusCount);
		userCountInfoDto.setCurrentCoinCount(userInfo.getCurrentCoinCount());

		return userCountInfoDto;
	}

	@Override
	public Integer updateCoinCountInfo(String userId, Integer changeCount) {
		return userInfoMapper.updateCoinCount(userId, changeCount);
	}
}
