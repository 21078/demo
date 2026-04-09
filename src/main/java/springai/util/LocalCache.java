package springai.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

/**
 * 本地缓存工具类 - 使用Caffeine实现
 * 提供增、删、查等基本操作
 */
@Component
public class LocalCache {

    // 使用Caffeine缓存
    private static final Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(1000)  // 最大缓存条目数
            .expireAfterWrite(30, TimeUnit.MINUTES)  // 30分钟后过期
            .expireAfterAccess(10, TimeUnit.MINUTES)  // 10分钟未访问则过期
            .build();

    // ==================== 增 ====================

    /**
     * 存储数据到缓存
     * @param key 键
     * @param value 值
     */
    public static void put(String key, Object value) {
        cache.put(key, value);
    }

    // ==================== 删 ====================

    /**
     * 删除指定键的缓存
     * @param key 键
     */
    public static void remove(String key) {
        cache.invalidate(key);
    }

    /**
     * 根据pattern删除匹配的键
     * @param pattern 匹配模式（支持*通配符）
     * @return 删除的键数量
     */
    public static int removeByPattern(String pattern) {
        List<String> keysToRemove = findKeysByPattern(pattern);
        keysToRemove.forEach(cache::invalidate);
        return keysToRemove.size();
    }

    // ==================== 查 ====================

    /**
     * 根据键获取缓存值
     * @param key 键
     * @return 值，如果不存在返回null
     */
    public static Object get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 根据pattern查找匹配的值
     * @param pattern 匹配模式（支持*通配符）
     * @return 匹配的值列表
     */
    public static List<Object> findByPattern(String pattern) {
        List<String> matchedKeys = findKeysByPattern(pattern);
        List<Object> matchedValues = new ArrayList<>();

        // 根据匹配的键获取对应的值
        matchedKeys.forEach(key -> {
            Object value = cache.getIfPresent(key);
            if (value != null) {
                matchedValues.add(value);
            }
        });

        return matchedValues;
    }

        /**
     * 根据pattern查找匹配的键
     * @param pattern 匹配模式（支持*通配符）
     * @return 匹配的键列表
     */
    public static List<String> findKeysByPattern(String pattern) {
        // 将通配符pattern转换为正则表达式
        String regex = pattern.replace("*", ".*");
        List<String> matchedKeys = new ArrayList<>();

        // 遍历所有键进行匹配
        cache.asMap().keySet().forEach(key -> {
            if (key.matches(regex)) {
                matchedKeys.add(key);
            }
        });

        return matchedKeys;
    }
}