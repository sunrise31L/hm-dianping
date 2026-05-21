package com.hmdp.service.impl;

import ch.qos.logback.classic.turbo.TurboFilter;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.constant.MessageConstant;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    /**
     * 根据id查询店铺信息
     * @param id
     * @return
     */
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY,id,Shop.class,this::getById,RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY,id,Shop.class,this::getById,RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);

        if(shop == null){
            return Result.fail(MessageConstant.SHOP_NOT_EXIST);
        }
        // 返回
        return Result.ok(shop);
    }

/*    // 创建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    *//**
     * 利用逻辑过期解决缓存击穿
     * @param id
     * @return
     *//*
    public Shop queryWithLogicalExpire(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(id);

        // 1、从redis查询店铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2、判断缓存是否命中
        // 没有命中
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        // 3、命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4、判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            // 5、未过期，直接返回店铺信息
            return shop;
        }
        // 6、已过期，需要重建缓存
        // 7、重建缓存
        // 7.1获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 7.2成功，则开启独立线程，实现缓存重建
        if(isLock){
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try{
                    // 重建缓存
                    this.saveShop2Redis(id,20L);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unlock(lockKey);
                }
            });
        }
        // 8、返回店铺信息
        return shop;
    }*/

/*    *//**
     * 利用互斥锁解决缓存击穿
     * @param id
     * @return
     *//*
    public Shop queryWithMutex(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1、从redis查询店铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2、判断缓存是否命中
        if(StrUtil.isNotBlank(shopJson)){
            log.info("命中Redis缓存,id为{}",id);
            // 3、如果命中，则直接返回
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        // 4、判断是否是命中空值
        if(shopJson != null){
            return null;
        }

        // 5、如果没有命中

        // 实现缓存重建
        // 尝试获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop;
        try {
            boolean isLock = tryLock(lockKey);
            // 判断是否获取互斥锁
            if(!isLock){
                // 失败，则休眠并重试
                Thread.sleep(10);
                return queryWithMutex(id);
            }
            // 6、成功，则根据id查数据库
            shop = getById(id);
            // 7、判断店铺是否存在
            if(shop == null){
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 8、如果店铺存在，则将店铺数据写入redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e){
            throw new RuntimeException(e);
        } finally {
            // 9、释放互斥锁
            unlock(lockKey);
        }

        // 10、返回店铺信息
        return shop;
    }*/

/*    *//**
     * 实现缓存穿透
     * @param id
     * @return
     *//*
    public Shop queryWithPassThrough(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1、从redis查询店铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2、判断缓存是否命中
        if(StrUtil.isNotBlank(shopJson)){
            log.info("命中Redis缓存,id为:{}",id);
            // 3、如果命中，则直接返回
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        // 4、如果没有命中
        // 4.1判断是否是命中空值
        if(shopJson != null){
            return null;
        }

        // 4.2根据id查询数据库
        Shop shop = getById(id);
        // 5、判断店铺是否存在
        if(shop == null){
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 7、如果店铺存在，则将店铺数据写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 8、返回店铺信息
        return shop;
    }*/

/*
    // 创建锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }*/

/*    //释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }*/

    private void saveShop2Redis(Long id, Long expireSeconds){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1、查询店铺数据
        Shop shop = new Shop();
        // 2、封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3、写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));// 永久有效
    }

    /**
     * 更新店铺信息
     * @param shop
     * @return
     */
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        if(id == null){
            return Result.fail(MessageConstant.SHOP_ID_NOT_NULL);
        }
        // 更新数据库
        updateById(shop);
        // 删除缓存
        stringRedisTemplate.delete(key);
        return Result.ok();
    }
}
