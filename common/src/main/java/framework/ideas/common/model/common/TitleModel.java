package framework.ideas.common.model.common;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class TitleModel implements MultiItemEntity {

    public static final int TITLE_HEAD = 401;
    public static int TITLE_SELECT_FRIEND = 402;
    public static int TITLE_SELECT_GROUP = 403;

    private int mType = 0;
    private String mTitle = "";
    private int mDrawable = 0;
    private Boolean mIsSpace = false;

    public TitleModel(String title,int type,int drawable){
        this.mTitle = title;
        this.mType = type;
        this.mDrawable = drawable;
    }

    public TitleModel(String title,int type,int drawable,Boolean isSpace){
        this.mTitle = title;
        this.mType = type;
        this.mDrawable = drawable;
        this.mIsSpace = isSpace;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public int getDrawable() {
        return mDrawable;
    }

    public Boolean getmIsSpace() {
        return mIsSpace;
    }

    public void setDrawable(int mDrawable) {
        this.mDrawable = mDrawable;
    }

    @Override
    public int getItemType() {
        return mType;
    }
}
