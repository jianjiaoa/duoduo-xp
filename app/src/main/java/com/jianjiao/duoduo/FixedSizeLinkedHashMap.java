package com.jianjiao.duoduo;

import java.util.LinkedHashMap;
import java.util.Map;

public class FixedSizeLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public FixedSizeLinkedHashMap(int maxSize, boolean accessOrder) {
        // 初始容量 = maxSize + 1，负载因子 = 1.0，避免在淘汰前触发扩容
        super(maxSize + 1, 1.0f, accessOrder);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // 当超过最大容量时，自动删除最旧元素
        return size() > maxSize;
    }
}
