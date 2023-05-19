package framework.ideas.common.model.common;

import com.chad.library.adapter.base.entity.MultiItemEntity;

public class SearchMergeChatModel implements MultiItemEntity {

    public static final int SEARCH_MERGE_CHAT = 1124;

    private long indexId = 0L;
    private String chatContent = "";
    private int matchCount = 0;
    private int type;//文件类型

    public SearchMergeChatModel(long indexId, String chatContent,int matchCount,int type){
        this.indexId = indexId;
        this.chatContent = chatContent;
        this.matchCount = matchCount;
        this.type = type;
    }

    public String getChatContent() {
        return chatContent;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setChatContent(String chatContent) {
        this.chatContent = chatContent;
    }

    public long getIndexId() {
        return indexId;
    }

    public void setIndexId(long indexId) {
        this.indexId = indexId;
    }

    public int getType() {
        return type;
    }

    @Override
    public int getItemType() {
        return SEARCH_MERGE_CHAT;
    }
}
