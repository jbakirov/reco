package models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Created by Baka on 26.09.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPage {
    private long id;
    private String username;
    private String profilePic;
    private String recosCount;
    private String questions;
    private String rAnswers;
    private String reputation;
    private String followers;
    private String following;
    private Boolean isFollowing;
    private List<FeedItem> feedItems;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRecosCount() {
        return recosCount;
    }

    public void setRecosCount(String recosCount) {
        this.recosCount = recosCount;
    }

    public String getQuestions() {
        return questions;
    }

    public void setQuestions(String questions) {
        this.questions = questions;
    }

    public String getrAnswers() {
        return rAnswers;
    }

    public void setrAnswers(String rAnswers) {
        this.rAnswers = rAnswers;
    }

    public String getReputation() {
        return reputation;
    }

    public void setReputation(String reputation) {
        this.reputation = reputation;
    }

    public String getFollowers() {
        return followers;
    }

    public void setFollowers(String followers) {
        this.followers = followers;
    }

    public String getFollowing() {
        return following;
    }

    public void setFollowing(String following) {
        this.following = following;
    }

    public List<FeedItem> getFeedItems() {
        return feedItems;
    }

    public void setFeedItems(List<FeedItem> feedItems) {
        this.feedItems = feedItems;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
}
