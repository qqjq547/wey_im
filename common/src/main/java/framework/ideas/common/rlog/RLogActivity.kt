package framework.ideas.common.rlog

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import framework.ideas.common.R
import framework.ideas.common.model.RLogModel
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.log.core.utils.TimeUtils
import io.realm.RealmChangeListener
import io.realm.RealmResults
import kotlinx.android.synthetic.main.common_activity_rlog.*

abstract class RLogActivity : BaseActivity() {

    private val mAdapter by lazy { LogsAdapter() }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.common_activity_rlog)

        clear.setOnClickListener {
            RLogManager.clear()
        }

        recycler_view_rlog.initSingleTypeRecycleView(
            LinearLayoutManager(this@RLogActivity),
            mAdapter,
            false
        )
        recycler_view_rlog.refreshController().setEnablePullToRefresh(false)

        RLogManager.getRLogHistory { result ->
            if (recycler_view_rlog != null) {
                mAdapter.setNewData(result)
                recycler_view_rlog.post {
                    recycler_view_rlog.recyclerViewController().scrollToEnd()
                }
            }
        }
    }

    class LogsAdapter : BaseQuickAdapter<RLogModel, BaseViewHolder>(R.layout.common_item_log) {

        override fun convert(helper: BaseViewHolder, item: RLogModel?) {
            item?.let {
                (helper.getView<TextView>(R.id.text).paint as TextPaint).isFakeBoldText =
                    item.level == RLogModel.LOG_LEVEL_I

                when (item.level) {
                    RLogModel.LOG_LEVEL_I,
                    RLogModel.LOG_LEVEL_D -> {
                        helper.getView<TextView>(R.id.text).setTextColor(Color.WHITE)
                    }
                    RLogModel.LOG_LEVEL_W -> {
                        helper.getView<TextView>(R.id.text).setTextColor(Color.YELLOW)
                    }
                    RLogModel.LOG_LEVEL_E -> {
                        helper.getView<TextView>(R.id.text).setTextColor(Color.RED)
                    }
                }

                if (!TextUtils.isEmpty(item.mark)) {
                    helper.getView<TextView>(R.id.text).text = "${
                        TimeUtils.format(
                            item.time,
                            "yyyy-MM-dd HH:mm:ss"
                        )
                    }/${item.tag}:${item.log} error -----> ${item.mark}"
                } else {
                    helper.getView<TextView>(R.id.text).text = "${
                        TimeUtils.format(
                            item.time,
                            "yyyy-MM-dd HH:mm:ss"
                        )
                    }/${item.tag}:${item.log}"
                }
            }
        }
    }

    protected abstract fun onClickSend()
}
