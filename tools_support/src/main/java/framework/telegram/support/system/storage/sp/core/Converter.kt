package framework.telegram.support.system.storage.sp.core

import java.io.IOException
import java.lang.reflect.Type

interface Converter<F, T> {

    @Throws(IOException::class)
    fun convert(value: F?): T?

    interface Factory {

        fun <F> fromType(fromType: Type): Converter<F, String>

        fun <T> toType(toType: Type): Converter<String, T>
    }
}