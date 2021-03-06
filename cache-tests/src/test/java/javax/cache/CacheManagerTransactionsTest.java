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
import org.junit.rules.MethodRule;

import javax.cache.transaction.IsolationLevel;
import javax.cache.transaction.Mode;
import javax.cache.util.AllTestExcluder;
import javax.cache.util.ExcludeListExcluder;
import javax.transaction.UserTransaction;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for CacheManagers that support transactions
 * <p/>
 *
 * @author Greg Luck
 * @since 1.0
 */
public class CacheManagerTransactionsTest extends TestSupport {

    /**
     * Rule used to exclude tests that do not implement Transactions
     */
    @Rule
    public MethodRule rule =
            Caching.isSupported(OptionalFeature.TRANSACTIONS) ?
                    new ExcludeListExcluder(this.getClass()) :
                    new AllTestExcluder();


    @Test
    public void transactionalStatusWhenNoUserTransaction() throws Exception {
        CacheManager cacheManager = getCacheManager();
        UserTransaction userTrans = cacheManager.getUserTransaction();
        assertEquals(javax.transaction.Status.STATUS_NO_TRANSACTION , userTrans.getStatus());
    }

    /**
     * The isolation level returned by a non-transactional cache
     */
    @Test
    public void isolationLevelForNonTransactionalCache() throws Exception {
        CacheManager cacheManager = getCacheManager();
        Cache cache = cacheManager.createCacheBuilder("test").build();
        assertEquals(IsolationLevel.NONE, cache.getConfiguration().getTransactionIsolationLevel());
    }

    /**
     * Test various illegal combinations
     */
    @Test
    public void setIncorrectIsolationLevelForTransactionalCache() throws Exception {
        CacheManager cacheManager = getCacheManager();
        try {
            cacheManager.createCacheBuilder("test").setTransactionEnabled(IsolationLevel.NONE, Mode.NONE).build();
        } catch (IllegalArgumentException e) {
            //expected
        }
        try {
            cacheManager.createCacheBuilder("test").setTransactionEnabled(IsolationLevel.READ_COMMITTED, Mode.NONE).build();
        } catch (IllegalArgumentException e) {
            //expected
        }
        try {
            cacheManager.createCacheBuilder("test").setTransactionEnabled(IsolationLevel.NONE, Mode.LOCAL).build();
        } catch (IllegalArgumentException e) {
            //expected
        }
    }




}
