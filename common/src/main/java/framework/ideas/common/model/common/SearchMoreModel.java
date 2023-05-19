package framework.ideas.common.model.common;

import com.chad.library.adapter.base.entity.MultiItemEntity;

public class SearchMoreModel implements MultiItemEntity {

    public static final int TITLE_MORE_CONTACTS = 406;//
    public static final int TITLE_MORE_GROUP = 407;
    public static final int TITLE_MORE_CHAT = 408;

    private int mType = 0;
    private String mTitle = "";

    public SearchMoreModel(String title, int type){
        this.mTitle = title;
        this.mType = type;
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


    @Override
    public int getItemType() {
        return mType;
    }
}
