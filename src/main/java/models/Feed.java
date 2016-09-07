package models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Baka on 07.08.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Feed {
    private FeedItem[] feedItem;

    public FeedItem[] getFeedItem() {
        return feedItem;
    }

    public void setFeedItem(FeedItem[] feedItem) {
        this.feedItem = feedItem;
    }
}
