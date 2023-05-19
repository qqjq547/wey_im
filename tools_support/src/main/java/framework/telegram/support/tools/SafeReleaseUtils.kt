package framework.telegram.support.tools

import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import java.io.Closeable
import java.io.Flushable

class SafeReleaseUtils {

    companion object {
        fun close(s: SQLiteOpenHelper?) {
            if (s != null) {
                try {
                    s.close()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        fun close(c: Closeable?) {
            if (c != null) {
                try {
                    if (c is Flushable) {
                        c.flush()
                    }
                    c.close()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        fun recycle(bitmap: Bitmap?) {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}