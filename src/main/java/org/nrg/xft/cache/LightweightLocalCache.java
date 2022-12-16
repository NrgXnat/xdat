package org.nrg.xft.cache;

import com.google.common.collect.Maps;
import org.nrg.prefs.beans.AbstractPreferenceBean;
import org.nrg.xft.schema.XFTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Map;

public class LightweightLocalCache<T> {
    private static final Logger log = LoggerFactory.getLogger(LightweightLocalCache.class);

    //loading each site config property can take a few milliseconds.  For common ones like logo path, site id, etc this adds up.  So, we can cache them for a minute or so.
    public static class LocalCache<T>{
        T value;
        Long lastCheckInMillis;

        public LocalCache(T v){
            this.value=v;
            this.lastCheckInMillis= Calendar.getInstance().getTimeInMillis();
        }
    }

    public T getValue(String key, Integer timeoutInMillis){
        if(XFTManager.isComplete() && key !=null && localCache.containsKey(key)){
            LocalCache cache = localCache.get(key);
            if((cache.lastCheckInMillis + timeoutInMillis) > Calendar.getInstance().getTimeInMillis()){
                return (T) cache.value;
            }
        }
        return null;
    }

    public T cacheValue(String key, T value){
        if(key!=null && value != null && localCache!=null && XFTManager.isInitialized()){
            LocalCache<T> lc = new LocalCache(value);
            localCache.put(key,lc);
        }
        return value;
    }

    private Map<String, LocalCache> localCache= Maps.newConcurrentMap();
}
