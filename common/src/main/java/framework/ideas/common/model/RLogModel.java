package framework.ideas.common.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RLogModel extends RealmObject {

    public static final int LOG_LEVEL_I = 1;
    public static final int LOG_LEVEL_D = 2;
    public static final int LOG_LEVEL_W = 3;
    public static final int LOG_LEVEL_E = 4;

    public static RLogModel create(String tag, String log, String mark, int level, long time, long sort) {
        RLogModel model = new RLogModel();
        model.id = System.nanoTime();
        model.tag = tag;
        model.log = log;
        model.mark = mark;
        model.level = level;
        model.time = time;
        model.sort = sort;
        return model;
    }

    public RLogModel copyModel() {
        RLogModel model = new RLogModel();
        model.id = id;
        model.tag = tag;
        model.log = log;
        model.mark = mark;
        model.level = level;
        model.time = time;
        model.sort = sort;
        return model;
    }

    @PrimaryKey
    private long id = 0;

    private long time = 0;

    private int level = 0;

    private String tag;

    private String log;

    private String mark;

    private long sort = 0;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getMark() {
        return mark;
    }

    public long getSort() {
        return sort;
    }

    public void setSort(long sort) {
        this.sort = sort;
    }
}
