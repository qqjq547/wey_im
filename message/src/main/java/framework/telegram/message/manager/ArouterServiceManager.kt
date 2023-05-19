package framework.telegram.message.manager

import com.alibaba.android.arouter.launcher.ARouter
import framework.telegram.business.bridge.service.*
import framework.telegram.message.bridge.service.IMessageService

object ArouterServiceManager {

    val systemService: ISystemService by lazy { ARouter.getInstance().navigation(ISystemService::class.java) }

    val contactService: IContactService by lazy { ARouter.getInstance().navigation(IContactService::class.java) }

    val groupService: IGroupService by lazy { ARouter.getInstance().navigation(IGroupService::class.java) }

    val settingService: ISettingService by lazy { ARouter.getInstance().navigation(ISettingService::class.java) }

    val messageService: IMessageService by lazy { ARouter.getInstance().navigation(IMessageService::class.java) }

    val qrService: IQrService by lazy { ARouter.getInstance().navigation(IQrService::class.java) }

    val searchService: ISearchService by lazy { ARouter.getInstance().navigation(ISearchService::class.java) }
}
