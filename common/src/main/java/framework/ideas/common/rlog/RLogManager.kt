package framework.ideas.common.rlog

import framework.ideas.common.model.RLogModel
import framework.telegram.support.system.log.AppLogcat
import io.realm.Realm
import io.realm.Sort
import java.util.concurrent.CopyOnWriteArrayList

object RLogManager {

    private var realmCallback: (((Realm) -> Unit, (() -> Unit)?, ((Throwable) -> Unit)?) -> Unit)? =
        null

    private val cache by lazy { CopyOnWriteArrayList<RLogModel>() }

    private var maxCacheSize = 100

    private val logger by lazy { AppLogcat.newLogger(1) }

    public fun init(
        realmCallback: ((Realm) -> Unit, (() -> Unit)?, ((Throwable) -> Unit)?) -> Unit
    ) {
        this.realmCallback = realmCallback
    }

    public fun i(tag: String, log: String) {
        logger.i(tag, log)

        save(
            RLogModel.create(
                tag,
                log,
                "",
                RLogModel.LOG_LEVEL_I,
                System.currentTimeMillis(),
                System.nanoTime()
            )
        )
    }

    public fun d(tag: String, log: String) {
        logger.d(tag, log)

        save(
            RLogModel.create(
                tag,
                log,
                "",
                RLogModel.LOG_LEVEL_D,
                System.currentTimeMillis(),
                System.nanoTime()
            )
        )
    }

    public fun w(tag: String, log: String) {
        logger.w(tag, log)

        save(
            RLogModel.create(
                tag,
                log,
                "",
                RLogModel.LOG_LEVEL_W,
                System.currentTimeMillis(),
                System.nanoTime()
            )
        )
    }

    public fun e(tag: String, log: String) {
        e(tag, log, null)
    }

    public fun e(tag: String, t: Throwable) {
        e(tag, "", t)
    }

    public fun e(tag: String, log: String, t: Throwable?) {
        if (t != null) {
            AppLogcat.logger.e(tag, t, log)
        } else {
            AppLogcat.logger.e(tag, log)
        }

        save(
            RLogModel.create(
                tag, log, t?.message
                    ?: "", RLogModel.LOG_LEVEL_E, System.currentTimeMillis(), System.nanoTime()
            )
        )
    }

    public fun clear() {
        realmCallback?.invoke({
            it.delete(RLogModel::class.java)
        }, {}, {})
    }

    public fun getRLogHistory(callback: ((List<RLogModel>) -> Unit)?) {
        val list = arrayListOf<RLogModel>()
        realmCallback?.invoke({ r ->
            val result = r.where(RLogModel::class.java)
                ?.sort("time", Sort.ASCENDING, "sort", Sort.ASCENDING)
                ?.findAll()
            if (result?.isNotEmpty() == true) {
                result.forEach {
                    list.add(it.copyModel())
                }
            }
        }, {
            callback?.invoke(list)
        }, {
            callback?.invoke(list)
        })
    }

    private var lastModelId = 0L

    @Synchronized
    private fun save(model: RLogModel) {
        if (lastModelId != model.id) {
            lastModelId = model.id

            cache.add(model)
            if (cache.size >= maxCacheSize) {
                val saveModels = ArrayList<RLogModel>(cache)
                cache.clear()
                realmCallback?.invoke({
                    it.copyToRealm(saveModels)

                    // 只保留24个小时以内的log
                    it.where(RLogModel::class.java)
                        .lessThan("time", System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                        .findAll()
                        .deleteAllFromRealm()
                }, {}, {})
            }
        }
    }
}