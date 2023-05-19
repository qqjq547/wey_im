package framework.telegram.support.system.storage.sp.core

import android.content.Context
import framework.telegram.support.system.storage.sp.core.Utils.validateServiceInterface
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

class IdeasPreference(private var converterFactory: Converter.Factory = DefaultConverterFactory()) {

    private val methodInfoCache = ConcurrentHashMap<Method, MethodInfo>()

    fun <T> create(context: Context, service: Class<T>): T {
        return create(context, service, "default")
    }

    fun <T> create(context: Context, service: Class<T>, stroageName: String): T {
        validateServiceInterface(service)
        return createServiceProxy(service, Preference(stroageName + "_" + getSpName(service), context, converterFactory))
    }

    private fun <T> createServiceProxy(service: Class<T>, preference: Preference): T =
            Proxy.newProxyInstance(service.classLoader, arrayOf(service), object : InvocationHandler {
                @Throws(Throwable::class)
                override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
                    return if (method.declaringClass == Any::class.java) {
                        method.invoke(this, args)
                    } else adapterMethod(preference, getMethodInfo(method, args), args)
                }
            }) as T

    private fun getSpName(clazz: Class<*>) = clazz.getAnnotation(SpName::class.java)?.value
            ?: clazz.simpleName

    private fun adapterMethod(preference: Preference, methodInfo: MethodInfo, args: Array<Any>?): Any? {
        return when (methodInfo.actionType) {
            GET -> preference.getValue(methodInfo.key, methodInfo.returnType, args?.getOrNull(0))
            PUT -> preference.putValue(methodInfo.key, args?.getOrNull(0))
            REMOVE -> preference.remove(methodInfo.key)
            CLEAR -> preference.clear()
            else -> null
        }
    }

    private fun getMethodInfo(method: Method, args: Array<Any>?): MethodInfo {
        var methodInfo: MethodInfo? = methodInfoCache[method]
        if (methodInfo == null) {
            methodInfo = MethodInfo(method, args)
            methodInfoCache[method] = methodInfo
        }
        return methodInfo
    }

}