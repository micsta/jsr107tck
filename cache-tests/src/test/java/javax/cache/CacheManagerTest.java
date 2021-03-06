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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.cache.util.ExcludeListExcluder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for CacheManager
 * <p/>
 *
 * @author Yannis Cosmadopoulos
 * @since 1.0
 */
public class CacheManagerTest extends TestSupport {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    
    /**
     * Rule used to exclude tests
     */
    @Rule
    public ExcludeListExcluder rule = new ExcludeListExcluder(this.getClass()) {

        /* (non-Javadoc)
         * @see javax.cache.util.ExcludeListExcluder#isExcluded(java.lang.String)
         */
        @Override
        protected boolean isExcluded(String methodName) {
            if ("testUnwrap".equals(methodName) && getUnwrapClass(CacheManager.class) == null) {
                return true;
            }
            
            return super.isExcluded(methodName);
        }
    };

    @Before
    public void startUp() {
        try {
            Caching.close();
        }   catch (CachingShutdownException e) {
            //this will happen if we call close twice in a row.
        }
    }

    @Test
    public void createCacheBuilder_NullCacheName() {
        CacheManager cacheManager = getCacheManager();
        try {
            cacheManager.createCacheBuilder(null);
            fail("should have thrown an exception - null cache name not allowed");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void createCache_Same() {
        String name = "c1";
        CacheManager cacheManager = getCacheManager();
        Cache cache = cacheManager.createCacheBuilder(name).build();
        assertSame(cache, cacheManager.getCache(name));
    }

    @Test
    public void createCache_NameOK() {
        String name = "c1";
        Cache cache = getCacheManager().createCacheBuilder(name).build();
        assertEquals(name, cache.getName());
    }

    @Test
    public void createCache_StatusOK() {
        String name = "c1";
        Cache cache = getCacheManager().createCacheBuilder(name).build();
        assertSame(Status.STARTED, cache.getStatus());
    }

    @Test
    public void createCache_Different() {
        String name1 = "c1";
        CacheManager cacheManager = getCacheManager();
        Cache cache1 = cacheManager.createCacheBuilder(name1).build();
        assertEquals(Status.STARTED, cache1.getStatus());

        String name2 = "c2";
        Cache cache2 = cacheManager.createCacheBuilder(name2).build();
        assertEquals(Status.STARTED, cache2.getStatus());

        assertEquals(cache1, cacheManager.getCache(name1));
        assertEquals(cache2, cacheManager.getCache(name2));
    }

    @Test
    public void createCache_DifferentSameName() {
        CacheManager cacheManager = getCacheManager();
        String name1 = "c1";
        Cache cache1 = cacheManager.createCacheBuilder(name1).build();
        assertEquals(cache1, cacheManager.getCache(name1));
        checkStarted(cache1);

        try {
            Cache cache2 = cacheManager.createCacheBuilder(name1).build();
        } catch (CacheException e) {
            //expected
        }
    }

    @Test
    public void removeCache_Null() {
        CacheManager cacheManager = getCacheManager();
        try {
            cacheManager.removeCache(null);
            fail("should have thrown an exception - cache name null");
        } catch (NullPointerException e) {
            //good
        }
    }

    @Test
    public void removeCache_There() {
        CacheManager cacheManager = getCacheManager();
        String name1 = "c1";
        cacheManager.createCacheBuilder(name1).build();
        assertTrue(cacheManager.removeCache(name1));
        assertFalse(cacheManager.getCaches().iterator().hasNext());
    }

    @Test
    public void removeCache_CacheStopped() {
        CacheManager cacheManager = getCacheManager();
        String name1 = "c1";
        Cache cache1 = cacheManager.createCacheBuilder(name1).build();
        cacheManager.removeCache(name1);
        checkStopped(cache1);
    }

    @Test
    public void removeCache_NotThere() {
        CacheManager cacheManager = getCacheManager();
        assertFalse(cacheManager.removeCache("c1"));
    }

    @Test
    public void removeCache_Stopped() {
        CacheManager cacheManager = getCacheManager();
        cacheManager.shutdown();
        try {
            cacheManager.removeCache("c1");
            fail();
        } catch (IllegalStateException e) {
            //ok
        }
    }

    @Test
    public void shutdown_stopCalled() {
        CacheManager cacheManager = getCacheManager();

        Cache cache1 = cacheManager.createCacheBuilder("c1").build();
        Cache cache2 = cacheManager.createCacheBuilder("c2").build();

        cacheManager.shutdown();

        checkStopped(cache1);
        checkStopped(cache2);
    }

    @Test
    public void shutdown_status() {
        CacheManager cacheManager = getCacheManager();

        assertEquals(Status.STARTED, cacheManager.getStatus());
        cacheManager.shutdown();
        assertEquals(Status.STOPPED, cacheManager.getStatus());
    }

    @Test
    public void shutdown_statusTwice() {
        CacheManager cacheManager = getCacheManager();

        cacheManager.shutdown();
        try {
            cacheManager.shutdown();
            fail();
        } catch (IllegalStateException e) {
            // good
        }
    }

    @Test
    public void shutdown_cachesEmpty() {
        CacheManager cacheManager = getCacheManager();

        cacheManager.createCacheBuilder("c1").build();
        cacheManager.createCacheBuilder("c2").build();

        cacheManager.shutdown();
        assertFalse(cacheManager.getCaches().iterator().hasNext());
    }

    @Test
    public void getUserTransaction() {
        boolean transactions = Caching.isSupported(OptionalFeature.TRANSACTIONS);
        try {
            getCacheManager().getUserTransaction();
            if (!transactions) {
                fail();
            }
        } catch (UnsupportedOperationException e) {
            assertFalse(transactions);
        }
    }

    @Test
    public void getCache_Missing() {
        CacheManager cacheManager = getCacheManager();
        assertNull(cacheManager.getCache("notThere"));
    }

    @Test
    public void getCache_There() {
        String name = this.toString();
        CacheManager cacheManager = getCacheManager();
        Cache cache = cacheManager.createCacheBuilder(name).build();
        assertSame(cache, cacheManager.getCache(name));
    }

    @Test
    public void getCache_Missing_Stopped() {
        CacheManager cacheManager = getCacheManager();
        cacheManager.shutdown();
        try {
            cacheManager.getCache("notThere");
            fail();
        } catch (IllegalStateException e) {
            //good
        }
    }

    @Test
    public void getCache_There_Stopped() {
        String name = this.toString();
        CacheManager cacheManager = getCacheManager();
        cacheManager.createCacheBuilder(name).build();
        cacheManager.shutdown();
        try {
            cacheManager.getCache(name);
            fail();
        } catch (IllegalStateException e) {
            //good
        }
    }

    @Test
    public void getCaches_Empty() {
        CacheManager cacheManager = getCacheManager();
        assertFalse(cacheManager.getCaches().iterator().hasNext());
    }

    @Test
    public void getCaches_NotEmpty() {
        CacheManager cacheManager = getCacheManager();

        ArrayList<Cache> caches1 = new ArrayList<Cache>();
        caches1.add(cacheManager.createCacheBuilder("c1").build());
        caches1.add(cacheManager.createCacheBuilder("c2").build());
        caches1.add(cacheManager.createCacheBuilder("c3").build());

        checkCollections(caches1, cacheManager.getCaches());
    }

    @Test
    public void getCaches_MutateReturn() {
        CacheManager cacheManager = getCacheManager();

        cacheManager.createCacheBuilder("c1").build();

        try {
            cacheManager.getCaches().iterator().remove();
            fail();
        } catch (UnsupportedOperationException e) {
            // immutable
        }
    }

    @Test
    public void getCaches_MutateCacheManager() {
        CacheManager cacheManager = getCacheManager();

        String removeName = "c2";
        ArrayList<Cache> caches1 = new ArrayList<Cache>();
        caches1.add(cacheManager.createCacheBuilder("c1").build());
        cacheManager.createCacheBuilder(removeName).build();
        caches1.add(cacheManager.createCacheBuilder("c3").build());

        Iterable<Cache<?, ?>> it;
        int size;

        it = cacheManager.getCaches();
        size = 0;
        for (Cache<?, ?> c : it) {
            size++;
        }
        assertEquals(3, size);
        cacheManager.removeCache(removeName);
        size = 0;
        for (Cache<?, ?> c : it) {
            size++;
        }
        assertEquals(3, size);

        it = cacheManager.getCaches();
        size = 0;
        for (Cache<?, ?> c : it) {
            size++;
        }
        assertEquals(2, size);
        checkCollections(caches1, it);
    }

    @Test
    public void isSupported() {
        CacheManager cacheManager = getCacheManager();

        for (OptionalFeature feature : OptionalFeature.values()) {
            assertSame(feature.toString(), Caching.isSupported(feature), cacheManager.isSupported(feature));
        }
    }

    @Test
    public void testUnwrap() {
        //Assumes rule will exclude this test when no unwrapClass is specified
        final Class<?> unwrapClass = getUnwrapClass(CacheManager.class);
        final CacheManager cacheManager = getCacheManager();
        final Object unwrappedCacheManager = cacheManager.unwrap(unwrapClass);
        
        assertTrue(unwrapClass.isAssignableFrom(unwrappedCacheManager.getClass()));
    }

    // ---------- utilities ----------

    private <T> void  checkCollections(Collection<T> collection1, Iterable<?> iterable2) {
        ArrayList<Object> collection2 = new ArrayList<Object>();
        for (Object element : iterable2) {
            assertTrue(collection1.contains(element));
            collection2.add(element);
        }
        assertEquals(collection1.size(), collection2.size());
        for (T element : collection1) {
            assertTrue(collection2.contains(element));
        }
    }

    private void checkStarted(Cache cache) {
        Status status = cache.getStatus();
        //may be asynchronous
        assertTrue(status == Status.STARTED);
    }

    private void checkStopped(Cache cache) {
        Status status = cache.getStatus();
        //may be asynchronous
        assertTrue(status == Status.STOPPED);
    }
}
