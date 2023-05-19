package framework.telegram.support.system.pinyin

import android.text.TextUtils
import android.util.Log
import com.github.promeg.pinyinhelper.Pinyin

class FastPinyin {

    companion object {

        fun toPinyin(c: CharSequence): String {
            val pinyin = Pinyin.toPinyin(c.first()).toUpperCase().first()
            return if (pinyin.isLetter()){
                pinyin.toString()
            }else{
                "#"
            }
        }

        fun toAllPinyinFirst(c: CharSequence,all:Boolean = false): String {//取汉子首字母
            var result = ""
            c.forEach {
                val pinyin = Pinyin.toPinyin(it).first()
                if (all){
                    result+=pinyin.toUpperCase()
                }else{
                    if (pinyin.isLetter()){
                        result+=pinyin.toUpperCase()
                    }
                }
            }
            return result
        }

        fun toAllPinyin(c: CharSequence): String {//取汉子
            var result = ""
            c.forEach {
                val pinyin = Pinyin.toPinyin(it)
                pinyin.forEach {letter->
                    if (letter.isLetter()){
                        result+=letter.toUpperCase()
                    }
                }
            }
            return result
        }


        fun isChinese(c: CharSequence): Boolean {
            return Pinyin.isChinese(c.first())
        }


        fun toPinyinChar(c: CharSequence): Char {
            val pinyin = Pinyin.toPinyin(c.first()).toUpperCase().first()
            return if (pinyin.isLetter()){
                pinyin
            }else{
                '#'
            }
        }

         /*
         * 1.找出第一个匹配的首字母
         * 2.截取第一个位置，匹配往后第一个匹配项
         * */
        fun findWordFromPinyin(pinyin:String,name:String):String{
             val allFirstPinyin = FastPinyin.toAllPinyinFirst(name,true).toUpperCase()
             val firstCase =pinyin[0].toUpperCase()
             val firstMatch = allFirstPinyin.indexOf(firstCase)
             if (firstMatch == -1)
                 return ""
             val allPinyin = FastPinyin.toAllPinyin(name)
             val first =allPinyin.indexOf(FastPinyin.toAllPinyin(name[firstMatch].toString()).toUpperCase())

             val start = allPinyin.indexOf(pinyin.toUpperCase(),first)
             val end = start + pinyin.length

             val matchMap = mutableMapOf<Int, String>()
             var offset = 0
             name.forEach {
                 val pinyin = FastPinyin.toAllPinyin(it.toString()).toUpperCase()
                 val start = allPinyin.indexOf(pinyin, offset)
                 offset = start + pinyin.length
                 val value = it.toString()
                 matchMap[start] = value
             }

             var str = ""
             for (index in start until end) {
                 val ss = matchMap[index]
                 if (ss != null && !TextUtils.isEmpty(ss)) {
                     str += ss
                 }
             }
             return str
        }

        fun findWordFromPinyinToFirst(pinyin:String,name:String):String{
            val allPinyin =  toAllPinyinFirst(name)
            val start = allPinyin.indexOf(pinyin.toUpperCase())
            val end = start + pinyin.length

            val matchMap = mutableMapOf<Int,String>()
            var offset = 0
            name.forEach {
                val pinyin = toAllPinyinFirst(it.toString()).toUpperCase()
                val start = allPinyin.indexOf(pinyin,offset)
                offset = start + pinyin.length
                val value = it.toString()
                matchMap[start] = value
            }

            var result = ""
            for (index in start until end){
                val ss = matchMap[index]
                if (ss !=null && !TextUtils.isEmpty(ss) ){
                    result += ss
                }
            }
            return result
        }

        fun isMatchFirst(keyword: String, name: String):Boolean{
            val allFirstPinyin = FastPinyin.toAllPinyinFirst(name).toUpperCase()
            val firstCase =keyword[0].toUpperCase()
            val firstMatch = allFirstPinyin.indexOf(firstCase)
            if (firstMatch == -1)
                return false
            return true
        }
    }
}