package framework.telegram.support.system.network.http

import io.reactivex.Observable
import io.reactivex.disposables.Disposable

inline fun <reified T> Observable<T>.httpLoad(noinline onNext: (t: T) -> Unit): Disposable {
    //处理异常返回
    return this.subscribe(onNext, {

    }, {

    })
}

inline fun <reified T> Observable<T>.httpLoad(noinline onNext: (t: T) -> Unit, noinline onError: (t: Throwable) -> Unit): Disposable {
    //处理异常返回
    return this.subscribe(onNext, onError, {

    })
}

inline fun <reified T> Observable<T>.httpLoad(noinline onNext: (t: T) -> Unit, noinline onError: (t: Throwable) -> Unit, noinline onComplete: () -> Unit): Disposable {
    return this.subscribe(onNext, onError, onComplete)
}