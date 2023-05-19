package framework.telegram.business.http

import java.lang.RuntimeException

class HttpException(val errCode: Int, val errMsg: String, val flag: String) : RuntimeException(errMsg)