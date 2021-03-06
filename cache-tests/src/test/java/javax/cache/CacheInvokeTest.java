/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package javax.cache;

import org.junit.Rule;
import org.junit.Test;

import javax.cache.util.ExcludeListExcluder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit test for Cache.
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class CacheInvokeTest extends CacheTestSupport<Integer, String> {
    /**
     * Rule used to exclude tests
     */
    @Rule
    public ExcludeListExcluder rule = new ExcludeListExcluder(this.getClass());

    @Test
    public void nullKey() {
        try {
            cache.invokeEntryProcessor(null, new MockEntryProcessor<Integer, String>());
            fail("null key");
        } catch (NullPointerException e) {
            //
        }
    }

    @Test
    public void nullProcessor() {
        try {
            cache.invokeEntryProcessor(123, null);
            fail("null key");
        } catch (NullPointerException e) {
            //
        }
    }

    @Test
    public void notStarted() {
        cache.stop();
        try {
            cache.invokeEntryProcessor(123, new MockEntryProcessor<Integer, String>());
            fail("null key");
        } catch (IllegalStateException e) {
            //
        }
    }

    @Test
    public void noValueNoMutation() {
        final Integer key = 123;
        final Integer ret = 456;
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertFalse(entry.exists());
                return ret;
            }
        };
        assertEquals(ret, cache.invokeEntryProcessor(key, processor));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void noValueSetValue() {
        final Integer key = 123;
        final Integer ret = 456;
        final String newValue = "abc";
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertFalse(entry.exists());
                entry.setValue(newValue);
                assertTrue(entry.exists());
                return ret;
            }
        };
        assertEquals(ret, cache.invokeEntryProcessor(key, processor));
        assertEquals(newValue, cache.get(key));
    }

    @Test
    public void noValueException() {
        final Integer key = 123;
        final String setValue = "abc";
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertFalse(entry.exists());
                entry.setValue(setValue);
                assertTrue(entry.exists());
                throw new IllegalAccessError();
            }
        };
        try {
            cache.invokeEntryProcessor(key, processor);
            fail();
        } catch (IllegalAccessError e) {
            //
        }
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void existingReplace() {
        final Integer key = 123;
        final String oldValue = "abc";
        final String newValue = "def";
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertTrue(entry.exists());
                String value1 = entry.getValue();
                assertEquals(oldValue, entry.getValue());
                entry.setValue(newValue);
                assertTrue(entry.exists());
                assertEquals(newValue, entry.getValue());
                return value1;
            }
        };
        cache.put(key, oldValue);
        assertEquals(oldValue, cache.invokeEntryProcessor(key, processor));
        assertEquals(newValue, cache.get(key));
    }

    @Test
    public void existingException() {
        final Integer key = 123;
        final String oldValue = "abc";
        final String newValue = "def";
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertTrue(entry.exists());
                String value1 = entry.getValue();
                assertEquals(oldValue, entry.getValue());
                entry.setValue(newValue);
                assertTrue(entry.exists());
                assertEquals(newValue, entry.getValue());
                throw new IllegalAccessError();
            }
        };
        cache.put(key, oldValue);
        try {
            cache.invokeEntryProcessor(key, processor);
            fail();
        } catch (IllegalAccessError e) {
            //
        }
        assertEquals(oldValue, cache.get(key));
    }

    @Test
    public void removeMissing() {
        final Integer key = 123;
        final Integer ret = 456;
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertFalse(entry.exists());
                entry.setValue("aba");
                assertTrue(entry.exists());
                entry.remove();
                assertFalse(entry.exists());
                return ret;
            }
        };
        assertEquals(ret, cache.invokeEntryProcessor(key, processor));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void removeThere() {
        final Integer key = 123;
        final String oldValue = "abc";
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertTrue(entry.exists());
                String oldValue = entry.getValue();
                entry.remove();
                assertFalse(entry.exists());
                return oldValue;
            }
        };
        cache.put(key, oldValue);
        assertEquals(oldValue, cache.invokeEntryProcessor(key, processor));
        assertFalse(cache.containsKey(key));
    }

    @Test
    public void removeException() {
        final Integer key = 123;
        final String oldValue = "abc";
        Cache.EntryProcessor<Integer, String> processor = new MockEntryProcessor<Integer, String>() {
            @Override
            public Object process(Cache.MutableEntry<Integer, String> entry) {
                assertTrue(entry.exists());
                entry.remove();
                assertFalse(entry.exists());
                throw new IllegalAccessError();
            }
        };
        cache.put(key, oldValue);
        try {
            cache.invokeEntryProcessor(key, processor);
            fail();
        } catch (IllegalAccessError e) {
            //
        }
        assertEquals(oldValue, cache.get(key));
    }

    @Test
    public void processorProcessor() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";
        final String value3 = "a3";

        cache.put(key, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new MyProcessorRunnable<Integer, String>(cache, key, value2, value3, 1L);
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorGet() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";

        cache.put(key, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.get(key);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value2, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorGetAll() throws Exception {
        final Integer key1 = 123;
        final String value1 = "a1";
        final String value2 = "a2";

        cache.put(key1, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key1, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                Set<Integer> keys = new HashSet<Integer>();
                keys.add(key1);
                return cache.getAll(keys);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value2, cache.get(key1));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, ((Map)r2.asynchResult.ret).get(key1));
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorContainsKey() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a1";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.containsKey(key);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value2, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(true, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorPut() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";
        final String value3 = "a3";

        cache.put(key, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                cache.put(key, value3);
                return value2;
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void putProcessor() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";
        final String value3 = "a3";

        AbstractRunnable r1 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                cache.put(key, value2);
                return value1;
            }
        };
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new MyProcessorRunnable<Integer, String>(cache, key, value2, value3, 1L);
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorGetAndPut() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";
        final String value3 = "a3";

        cache.put(key, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.getAndPut(key, value3);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void getAndPutProcessor() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";
        final String value3 = "a3";

        cache.put(key, value1);
        AbstractRunnable r1 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.getAndPut(key, value2);
            }
        };
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new MyProcessorRunnable<Integer, String>(cache, key, value2, value3, 1L);
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorPutAll() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";
        final String value3 = "a3";

        cache.put(key, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                HashMap<Integer, String> map = new HashMap<Integer, String>();
                map.put(key, value3);
                cache.putAll(map);
                return value2;
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorPutIfAbsent() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";
        final String value3 = "a3";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.putIfAbsent(key, value3);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value2, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(false, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorRemove() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.remove(key);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertFalse(cache.containsKey(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(true, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorRemove2() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.remove(key, value2);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertFalse(cache.containsKey(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(true, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorGetAndRemove() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.getAndRemove(key);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertFalse(cache.containsKey(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorReplace3() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";
        final String value3 = "a3";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.replace(key, value2, value3);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(true, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorReplace2() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";
        final String value3 = "a3";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.replace(key, value3);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(true, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorGetAndReplace() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";
        final String value3 = "a3";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                return cache.getAndReplace(key, value3);
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertEquals(value3, cache.get(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorRemoveAll1() throws Exception {
        final Integer key = 123;
        final String value1 = null;
        final String value2 = "a2";

        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                cache.removeAll(Arrays.asList(key));
                return value2;
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertFalse(cache.containsKey(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    @Test
    public void processorRemoveAll0() throws Exception {
        final Integer key = 123;
        final String value1 = "a1";
        final String value2 = "a2";

        cache.put(key, value1);
        AbstractRunnable r1 = new MyProcessorRunnable<Integer, String>(cache, key, value1, value2, 100L);
        Thread t1 = new Thread(r1);
        AbstractRunnable r2 = new AbstractRunnable() {
            @Override
            protected Object internalRun() {
                cache.removeAll();
                return value2;
            }
        };
        Thread t2 = new Thread(r2);
        t1.start();
        Thread.sleep(10L);
        t2.start();
        t1.join();
        t2.join();
        assertFalse(cache.containsKey(key));
        assertEquals(value1, r1.asynchResult.ret);
        assertEquals(value2, r2.asynchResult.ret);
        assertTrue(r2.asynchResult.outTime >= r1.asynchResult.outTime);
    }

    private static class MockEntryProcessor<K, V> implements Cache.EntryProcessor<K, V> {

        @Override
        public Object process(Cache.MutableEntry<K, V> kvMutableEntry) {
            throw new UnsupportedOperationException();
        }
    }

    private static class MyProcessorRunnable<K, V> extends AbstractRunnable {
        protected final Cache<K,V> cache;
        protected final K key;
        protected final V oldValue;
        protected final V newValue;
        protected final long sleep;

        public MyProcessorRunnable(Cache<K, V> cache, K key, V oldValue, V newValue, long sleep) {
            this.cache = cache;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.sleep = sleep;
        }

        @Override
        public Object internalRun() {
            Cache.EntryProcessor<K, V> processor = new MockEntryProcessor<K, V>() {

                @Override
                public Object process(Cache.MutableEntry<K, V> entry) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        //
                    }
                    entry.setValue(newValue);
                    return oldValue;
                }
            };
            cache.invokeEntryProcessor(key, processor);
            return oldValue;
        }
    }

    private abstract static class AbstractRunnable implements Runnable {
        protected final AsynchResult asynchResult = new AsynchResult();

        @Override
        public void run() {
            asynchResult.inTime = System.currentTimeMillis();
            try {
                asynchResult.ret = internalRun();
            } catch (Throwable t) {
                asynchResult.throwable = t;
            } finally {
                asynchResult.outTime = System.currentTimeMillis();
            }
        }

        protected abstract Object internalRun();
    }

    private static class AsynchResult {
        public long inTime;
        public long outTime;
        public Object ret;
        public Throwable throwable;
    }
}
