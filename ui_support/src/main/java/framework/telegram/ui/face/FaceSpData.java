package framework.telegram.ui.face;

import static framework.telegram.ui.face.IconFaceHandler.DATA;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lzh on 19-10-11.
 * INFO:
 */
public class FaceSpData {
    private List<IconFaceItem> mFaceList1;
    private List<IconFaceItem> mFaceList2;

    FaceSpData(List<IconFaceItem> faceList1, List<IconFaceItem> faceList2) {
        mFaceList1 = faceList1;
        mFaceList2 = faceList2;
    }

    public List<IconFaceItem> getmFaceList1() {//过滤掉旧数据
        ArrayList list = new ArrayList();
        for (int i = 0; i < mFaceList1.size(); i++) {
            IconFaceItem item = mFaceList1.get(i);
            if (item.resId != -1) {
                IconFaceItem realItem = DATA.get(item.name);
                item.resId = realItem.resId;
                if (!list.contains(item)) {
                    list.add(item);
                }
            } else {
                list.add(item);
            }
        }
        return list;
    }

    public void setmFaceList1(List<IconFaceItem> mFaceList1) {
        this.mFaceList1 = mFaceList1;
    }

    public List<IconFaceItem> getmFaceList2() {//过滤掉旧数据
        ArrayList list = new ArrayList();
        for (int i = 0; i < mFaceList2.size(); i++) {
            IconFaceItem item = mFaceList2.get(i);
            if (item.resId != -1) {
                IconFaceItem realItem = DATA.get(item.name);
                item.resId = realItem.resId;
                if (!list.contains(item)) {
                    list.add(item);
                }
            } else {
                list.add(item);
            }
        }
        return list;
    }

    public void setmFaceList2(List<IconFaceItem> mFaceList2) {
        this.mFaceList2 = mFaceList2;
    }
}
