package framework.telegram.support.system.cache.stategy;


import framework.telegram.support.system.cache.RxCache;
import framework.telegram.support.system.cache.data.CacheResult;

import org.reactivestreams.Publisher;

import java.lang.reflect.Type;

import framework.telegram.support.system.cache.RxCache;
import io.reactivex.Flowable;


/**
 * author : zchu
 * date   : 2017/10/11
 * desc   :
 */
public interface IFlowableStrategy {

    <T> Publisher<CacheResult<T>> flow(RxCache rxCache, String key, Flowable<T> source, Type type);
}
