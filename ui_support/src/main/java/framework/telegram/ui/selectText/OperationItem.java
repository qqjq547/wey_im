package framework.telegram.ui.selectText;

/**
 * Created by wangyang53 on 2018/3/28.
 */

public class OperationItem {
    public static final int ACTION_COPY         = 1;
    public static final int ACTION_SELECT_ALL   = 2;
    public static final int ACTION_CANCEL       = 3;
    public static final int ACTION_FORWARD      = 4;
    public static final int ACTION_REPLY        = 5;
    public static final int ACTION_DELETE       = 6;
    public static final int ACTION_MULTIPLE     = 7;
    public static final int ACTION_DETAIL       = 8;
    public static final int ACTION_SILENT       = 9;

    public String name;
    public int action;

    @Override
    public String toString() {
        return "OperationItem{" +
                "name='" + name + '\'' +
                ", action=" + action +
                '}';
    }
}
