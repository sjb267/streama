package com.streama.redis;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component("redisUtils")
public class RedisUtils<V> {

    @Resource
    private RedisTemplate<String, V> redisTemplate;

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    public void delete(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
            }
        }
    }

    public V get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, V value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean keyExists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean setex(String key, V value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.MILLISECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key  键
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.MILLISECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("设置redisKey:{}过期时间:{}失败", key, time);
            return false;
        }
    }

    /**
     * 获取队列中的所有值
     *
     * @param key 键
     * @return 队列中的所有值
     */
    public List<V> getQueueList(String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 队列左压入
     *
     * @param key   键
     * @param value 值
     * @param time  过期时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean lpush(String key, V value, Long time) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            if (time != null && time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("队列左压入key:{},value:{}失败", key, value);
            return false;
        }
    }

    /**
     * 队列左弹出
     *
     * @param key 键
     * @return 队列中的所有值
     */
    public V lpop(String key) {
        try {
            return redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.error("队列左弹出key:{}失败", key);
            return null;
        }
    }

    /**
     * 从队列中删除指定的值
     *
     * @param key   键
     * @param value 值
     * @return 删除的数量
     */
    public long remove(String key, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, 1, value);
            return remove;
        } catch (Exception e) {
            log.error("从队列中删除指定的值key:{},value:{}失败", key, value);
            return 0;
        }
    }

    /**
     * 队列左压入多个值
     *
     * @param key   键
     * @param values 值
     * @param time  过期时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean lpushAll(String key, List<V> values, long time) {
        try {
            redisTemplate.opsForList().leftPushAll(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("队列左压入key:{},value:{}失败", key, values);
            return false;
        }
    }

    /**
     * 队列右弹出
     *
     * @param key 键
     * @return 队列中的所有值
     */
    public V rpop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            log.error("队列右弹出key:{}失败", key);
            return null;
        }
    }

    /**
     * 计数器增加
     *
     * @param key 键
     * @return 增加后的数量
     */
    public Long increment(String key) {
        Long count = redisTemplate.opsForValue().increment(key, 1);
        return count;
    }

    /**
     * 计数器增加并设置过期时间
     *
     * @param key         键
     * @param milliseconds 过期时间(毫秒)
     * @return 增加后的数量
     */
    public Long incrementex(String key, long milliseconds) {
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count == 1) {
            //设置过期时间1天
            expire(key, milliseconds);
        }
        return count;
    }

    /**
     * 计数器减少
     *
     * @param key 键
     * @return 减少后的数量
     */
    public Long decrement(String key) {
        Long count = redisTemplate.opsForValue().increment(key, -1);
        if (count <= 0) {
            redisTemplate.delete(key);
        }
        log.info("key:{},减少数量{}", key, count);
        return count;
    }

    /**
     * 获取所有符合前缀的key
     *
     * @param keyPrifix 前缀
     * @return 所有符合前缀的key
     */
    public Set<String> getByKeyPrefix(String keyPrifix) {
        Set<String> keyList = redisTemplate.keys(keyPrifix + "*");
        return keyList;
    }

    /**
     * 批量获取符合前缀的key-value
     *
     * @param keyPrifix 前缀
     * @return 所有符合前缀的key-value
     */
    public Map<String, V> getBatch(String keyPrifix) {
        Set<String> keySet = redisTemplate.keys(keyPrifix + "*");
        List<String> keyList = new ArrayList<>(keySet);
        List<V> keyValueList = redisTemplate.opsForValue().multiGet(keyList);
        Map<String, V> resultMap = keyList.stream().collect(Collectors.toMap(key -> key, value -> keyValueList.get(keyList.indexOf(value))));
        return resultMap;
    }

    /**
     * 增加zset中元素的分数
     *
     * @param key 键
     * @param v   值
     */
    public void zaddCount(String key, V v) {
        redisTemplate.opsForZSet().incrementScore(key, v, 1);
    }


    /**
     * 获取zset中排名前count的元素
     *
     * @param key   键
     * @param count 数量
     * @return 排名前count的元素
     */
    public List<V> getZSetList(String key, Integer count) {
        Set<V> topElements = redisTemplate.opsForZSet().reverseRange(key, 0, count);
        List<V> list = new ArrayList<>(topElements);
        return list;
    }

}
