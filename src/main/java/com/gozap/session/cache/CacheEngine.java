package com.gozap.session.cache;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存引擎的接口
 * @version 1.0 2009.06.08
 * @since 1.6.2
 * @author Stone
 */
public interface CacheEngine {

    /**
     * 表示此缓存的值一经写入就不会被改变.
     * 至于使用与否将有实现类自行决定.
     */
    public static final String UNCHANGING_VALUE_FLAG = "[unchanging]";

    /**
     * 初始化引擎。
     * @param prop 配置属性。
     * @param servletContext
     */
    public void init(Properties prop, ServletContext servletContext);

    /**
     *
     */
    public void start();
    /**
     * 停止缓存，执行清空其中的内容等动作
     */
    public void stop();

    /**
     * 缓存中是否包含key
     * 找到返回为true,找不到返回为false
     * @param key 查找的值
     * @return 是否找到
     */
    public boolean containsKey(String key);

    /**
     * 向缓存中添加对象
     * @param key 添加对象的key
     * @param value 添加的对象
     * @return true更新成功,false更新失败.
     */
    public void put(String key, Object value);

    /**
     * 更新缓存中指定key的值
     * @param key 缓存key.
     * @param value 缓存的值.
     * @param seconds 缓存过期的秒数
     * @return
     */
    public void put(String key, Object value, int seconds);

    /**
     * 向缓存中添加对应的对象，并在对应组中登记
     * @param key 缓存的key.
     * @param value 缓存的值.
     * @param group 组名称列表
     * @return
     */
    public void put(String key, Object value, String[] group);

    /**
     * 取得缓存对象
     * @param key 缓存对象的key
     * @return 查询到的缓存的对象
     * @throws CacheNotFoundException 在缓存中不存在指定KEY所代表的值。
     */
    public Object get(String key);

    /**
     * 批量获取缓存中的对象.如果指定的key不存在于缓存中将不会包含在返回的哈希表中.
     * 总是会返回一个只读的Map的实例.
     *
     * @param keys 缓存的key列表.
     * @return 缓存key和值的哈希映射表.
     */
    public Map<String, Object> get(String[] keys);

    /**
     * 如果当前的key为long的数值那么会进行原子的增加操作。
     * 如果缓存不存在，那么将默认为0的基础上操作。
     * 如果只需要获取当前的值，建议使用getNumber方法获取。
     * @param key 需要操作的key.
     * @param magnitude 幅度。如果小于0将取绝对值.
     * @return 新的值。
     */
    public long increase(String key, long magnitude);

    /**
     * 对key指定的值进行原子减法操作。如果缓存不存在，那么将默认为0的基础上操作。
     * 数据最小为0，如果减去的数据会造成当前数据小于0那当前值作为0作理。
     * 比如当前值为10，减去20，那么结果为－20，当前值变为0。
     * 如果只需要获取当前的值，建议使用getNumber方法获取。
     * @param key 需要操作的key.
     * @param magnitude 幅度。如果小于0那么将取绝对值.
     * @return 新的值。
     */
    public long decrease(String key, long magnitude);

    /**
     * 删除缓存中对应的key的对象
     * @param key 缓存对象的key
     */
    public void remove(String key);

    /**
     * 返回当前缓存实例是否已经初始化完成.
     * @return true 完成,false没有完成.
     */
    public boolean isInitialized();
    
    /**
     * 清空对应的组
     * @param group 对应组的名称
     */
    public void flushGroup(String group);
}
