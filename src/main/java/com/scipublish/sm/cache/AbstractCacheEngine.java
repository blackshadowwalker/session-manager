package com.scipublish.sm.cache;

import java.util.Properties;

/**
 * 一个抽象缓存实现,实现了关于是否初始化的方法定义.
 * 子类只需要重写doInit和doStop方法即可,并不需要关心执行的时机.
 *
 * @version 1.00 2013-2-6 17:00:43
 * @since 1.5
 * @author mike
 */
public abstract class AbstractCacheEngine implements CacheEngine {

    private volatile boolean initialized = false;

    public void init(Properties prop) {
        if (isInitialized()) {
            return;

        } else {
            doInit(prop);
            initialized = true;
        }
    }

    public void stop() {
        if (isInitialized()) {

            doStop();

            initialized = false;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    protected abstract void doInit(Properties prop);

    protected abstract void doStop();
}
