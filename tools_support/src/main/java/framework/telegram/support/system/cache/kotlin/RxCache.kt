package framework.telegram.support.system.cache.kotlin

import com.google.gson.reflect.TypeToken
import framework.telegram.support.system.cache.RxCache
import framework.telegram.support.system.cache.data.CacheResult
import framework.telegram.support.system.cache.stategy.IFlowableStrategy
import framework.telegram.support.system.cache.stategy.IObservableStrategy
import io.reactivex.*

inline fun <reified T> framework.telegram.support.system.cache.RxCache.load(key: String): Observable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return load<T>(key, object : TypeToken<T>() {}.type)
}

inline fun <reified T> framework.telegram.support.system.cache.RxCache.load2Flowable(key: String): Flowable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return load2Flowable(key, object : TypeToken<T>() {}.type, BackpressureStrategy.LATEST)
}

inline fun <reified T> framework.telegram.support.system.cache.RxCache.load2Flowable(key: String, backpressureStrategy: BackpressureStrategy): Flowable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return load2Flowable(key, object : TypeToken<T>() {}.type, backpressureStrategy)
}

inline fun <reified T> framework.telegram.support.system.cache.RxCache.transformObservable(key: String, strategy: framework.telegram.support.system.cache.stategy.IObservableStrategy): ObservableTransformer<T, framework.telegram.support.system.cache.data.CacheResult<T>> {
    return transformObservable(key, object : TypeToken<T>() {}.type, strategy)
}

inline fun <reified T> framework.telegram.support.system.cache.RxCache.transformFlowable(key: String, strategy: framework.telegram.support.system.cache.stategy.IFlowableStrategy): FlowableTransformer<T, framework.telegram.support.system.cache.data.CacheResult<T>> {
    return transformFlowable(key, object : TypeToken<T>() {}.type, strategy)
}

inline fun <reified T> Observable<T>.applyCache(key: String, strategy: framework.telegram.support.system.cache.stategy.IObservableStrategy): Observable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return this.applyCache(framework.telegram.support.system.cache.RxCache.getDefault(), key, strategy)
}

inline fun <reified T> Observable<T>.applyCache(rxCache: framework.telegram.support.system.cache.RxCache, key: String, strategy: framework.telegram.support.system.cache.stategy.IObservableStrategy): Observable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return this.compose<framework.telegram.support.system.cache.data.CacheResult<T>>(rxCache.transformObservable(key, object : TypeToken<T>() {}.type, strategy))
}

inline fun <reified T> Flowable<T>.applyCache(key: String, strategy: framework.telegram.support.system.cache.stategy.IFlowableStrategy): Flowable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return this.applyCache(framework.telegram.support.system.cache.RxCache.getDefault(), key, strategy)
}

inline fun <reified T> Flowable<T>.applyCache(rxCache: framework.telegram.support.system.cache.RxCache, key: String, strategy: framework.telegram.support.system.cache.stategy.IFlowableStrategy): Flowable<framework.telegram.support.system.cache.data.CacheResult<T>> {
    return this.compose<framework.telegram.support.system.cache.data.CacheResult<T>>(rxCache.transformFlowable(key, object : TypeToken<T>() {}.type, strategy))
}






