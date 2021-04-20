package work.cxlm.filecase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import work.cxlm.filecase.cache.MultiStringCache;

import java.util.Objects;

/**
 * create 2021/4/20 10:59
 *
 * @author Chiru
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileCaseApplication.class)
public class MultiStringCacheTest {

    MultiStringCache multiStringCache;

    @Autowired
    public void setMultiCache(MultiStringCache multiStringCache) {
        this.multiStringCache = multiStringCache;
    }

    @After
    public void clearAll() {
        multiStringCache.clearAll();
    }

    @Test
    public void getCacheTest() {
        String key = "key";
        String defaultValue = "default";
        Assert.assertEquals(defaultValue, multiStringCache.getCache(key, defaultValue));
        Assert.assertNull(multiStringCache.getCache(key));
    }

    @Test
    public void setTest() {
        // 设置了就应该查得到
        String key1 = "key1";
        String val1 = "val1";
        multiStringCache.set(key1, val1);
        Assert.assertEquals(multiStringCache.getCache(key1), val1);

        // 设置在第 0 层，第 0 层有，第 1 层无，整体可以查到
        String key2 = "key2";
        String val2 = "val2";
        multiStringCache.set(key2, val2, 0);
        Assert.assertEquals(multiStringCache.getCache(key2), val2);
        Assert.assertEquals(multiStringCache.getFrom(key2, 0), val2);
        Assert.assertNull(multiStringCache.getFrom(key2, 1));

        // 设置在第 1 层，只有第 1 层有，第 0 层无，整体可以查到
        String key3 = "key3";
        String val3 = "val3";
        multiStringCache.set(key3, val3, 1);
        Assert.assertEquals(multiStringCache.getCache(key3), val3);
        Assert.assertEquals(multiStringCache.getFrom(key3, 1), val3);
        Assert.assertNull(multiStringCache.getFrom(key3, 0));
}

    @Test
    public void getWithSupplierTest() {
        // 获取后，Supplier 被调用，val1 被设置到缓存
        String key1 = "key1";
        String val1 = "val1";
        Assert.assertEquals(multiStringCache.get(key1, () -> val1), val1);
        Assert.assertEquals(multiStringCache.getCache(key1), val1);
    }

    @Test
    public void clearLayerTest() {
        String key1 = "key1";
        String val1 = "val1";
        multiStringCache.set(key1, val1);
        // 清除第一层后，整体能查到，第一层查不到
        multiStringCache.clearLayer(1);
        Assert.assertEquals(multiStringCache.getCache(key1), val1);
        Assert.assertNull(multiStringCache.getFrom(key1, 1));
        // 清除第零层后，都没了
        multiStringCache.clearLayer(0);
        Assert.assertNull(multiStringCache.getCache(key1));

        String key2 = "key2";
        String val2 = "val2";
        multiStringCache.set(key2, val2);
        // 清除全部后，都没了
        multiStringCache.clearAll();
        Assert.assertNull(multiStringCache.getCache(key2));
    }

    @Test
    public void deleteTest() {
        String key1 = "key1";
        String val1 = "val1";
        multiStringCache.set(key1, val1);
        String key2 = "key2";
        String val2 = "val2";
        multiStringCache.set(key2, val2);
        // 删除一个后，被删除的没了，没被删除的还在
        multiStringCache.delete(key1);
        Assert.assertNull(multiStringCache.getCache(key1));
        Assert.assertEquals(multiStringCache.getCache(key2), val2);
    }

    @Test
    public void getAndSetAnyTest() {
        String key1 = "key1";
        ValueClass val1 = new ValueClass();
        val1.num = 1;
        val1.val = "1";
        multiStringCache.setAny(key1, val1);
        ValueClass gotValue1 = multiStringCache.getAnyCache(key1, ValueClass.class);
        Assert.assertEquals(val1, gotValue1);

        String key2 = "key2";
        ValueClass val2 = new ValueClass();
        val2.num = 2;
        val2.val = "2";
        ValueClass gotValue2 = multiStringCache.getAny(key2, ValueClass.class, () -> val2);
        Assert.assertEquals(val2, gotValue2);
        ValueClass gotValue3 = multiStringCache.getAnyCache(key2, ValueClass.class);
        Assert.assertEquals(val2, gotValue3);
    }

    static class ValueClass {
        public String val;
        public int num;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValueClass that = (ValueClass) o;
            return num == that.num && Objects.equals(val, that.val);
        }
    }
}
