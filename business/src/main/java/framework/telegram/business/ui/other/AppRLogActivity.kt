package framework.telegram.business.ui.other

import com.alibaba.android.arouter.facade.annotation.Route
import framework.ideas.common.rlog.RLogActivity
import framework.telegram.business.bridge.Constant.ARouter.ROUNTE_SYSTEM_RLOG
import io.realm.Realm
import io.realm.RealmConfiguration

@Route(path = ROUNTE_SYSTEM_RLOG)
class AppRLogActivity : RLogActivity() {

    override fun onClickSend() {

    }
}
