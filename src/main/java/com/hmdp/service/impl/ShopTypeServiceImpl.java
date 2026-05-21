package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.constant.MessageConstant;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 店铺类型缓存
     * @return
     */
    public Result queryTypeList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        // 先查redis
        List<String> ShopTypeJson = stringRedisTemplate.opsForList().range(key,0,-1);
        // 如果有则直接返回
        if(CollectionUtil.isNotEmpty(ShopTypeJson)){
            log.info("店铺类型缓存");
            List<ShopType> shopTypes = ShopTypeJson.stream()
                    .map(json -> JSONUtil.toBean(json, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        // 如果没有就去查数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 如果数据库没有，则报错
        if(CollectionUtil.isEmpty(shopTypes)){
            return Result.fail(MessageConstant.SHOP_TYPE_NOT_EXIST);
        }
        // 查到后存入redis
        List<String> shopTypesJson = shopTypes.stream()
                .map(shopType -> JSONUtil.toJsonStr(shopType))
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(key, shopTypesJson);
        // 返回店铺类型
        return Result.ok(shopTypes);
    }
}
