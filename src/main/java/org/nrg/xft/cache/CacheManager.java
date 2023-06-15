/*
 * core: org.nrg.xft.cache.CacheManager
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xft.cache;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.services.cache.XnatCache;
import org.springframework.cache.support.SimpleValueWrapper;

public interface CacheManager extends XnatCache {
    static CacheManager _passthroughCache = new ExternalCache();

    static CacheManager GetInstance() {
        return _passthroughCache;
    }


    public static class ExternalCache implements CacheManager{
        @Override
        public String getCacheName() {
            return "xft-cache";
        }

        @Override
        public void clearAll() {
            XDAT.getCacheManager().getCache(getCacheName()).clear();
        }

        @Override
        public void clearXsiType(String xsiType) {
            //is there a way to do this?
            XDAT.getCacheManager().getCache(getCacheName()).clear();
        }

        @Override
        public Object retrieve(String xsiType, Object id) {
            SimpleValueWrapper obj = ((SimpleValueWrapper) XDAT.getCacheManager().getCache(getCacheName()).get(id));
            if(obj!=null){
                return obj.get();
            }

            return null;
        }

        @Override
        public Object put(String xsiType, Object id, Object item) {
            XDAT.getCacheManager().getCache(getCacheName()).put(id,item);
            return item;
        }

        @Override
        public Object remove(String xsiType, Object id) {
            Object temp = retrieve(xsiType,id);
            XDAT.getCacheManager().getCache(getCacheName()).evict(id);
            return temp;
        }
    }


    /**
     * Clears all entries in the cache.
     */
    void clearAll();

    /**
     * Clears all objects cached with the given XSI type.
     *
     * @param xsiType The XSI type of the cached objects to be cleared
     */
    void clearXsiType(String xsiType);

    /**
     * Retrieves the object cached with the given XSI type and object ID.
     *
     * @param xsiType The XSI type of the cached object
     * @param id      The ID of the cached object
     *
     * @return The cached object if it exists in the cache, <pre>null</pre> otherwise.
     */
    Object retrieve(String xsiType, Object id);

    /**
     * Puts the item into the cache with the specified XSI type and ID.
     *
     * @param xsiType The XSI type of the object to be cached
     * @param id      The ID of the object to be cached
     * @param item    The object to be cached
     */
    Object put(String xsiType, Object id, Object item);

    /**
     * Removes the item cached with the specified XSI type and ID from the cache. This returns the object if it exists in
     * the cache.
     *
     * @param xsiType The XSI type of the object to be cached
     * @param id      The ID of the object to be cached
     *
     * @return The cached object if it exists
     */
    Object remove(String xsiType, Object id);
}
