package models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Baka on 04.09.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeFeed {
    private long uid;
    private int page;
    private long ruid;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getRuid() {
        return ruid;
    }

    public void setRuid(long ruid) {
        this.ruid = ruid;
    }
}
