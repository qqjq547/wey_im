package framework.telegram.support.system.upload


/**
 * Created by lzh on 20-1-11.
 * INFO:
 */

//{"code":200,"desc":"成功","data":"/201911/jpg/d599c308fe3ba96783d98e1eeeee4e9b"}
data class CommonResult(val code: Long, val desc: String, val data: String)

data class LocalResult(val code: Long, val msg: String)
