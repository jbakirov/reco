package models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by Baka on 07.08.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL )
public class FeedItem {

    //    private Item item;

    private long userId;
    private String username;
    private String profilePic;
    private String type;
    private long postId;
    private long itemId;
    private String itemName;
    private String itemYear;
    private String info;
    private String itemAuthor;
    private String genre;

    private long genreId; // for yvideos
    private String channelName;
    private long channelId;

    private String citation;
    private String feedback;
    private String timestamp;  //when recommendation was created
    private String poster_url;
    private int commentCount;
    private boolean liked;
    private Long recosCount;
    private List<String> likers;
    private String trailerUrl;

    private Long directedToId;
    private String directedToUsername;

    private long likes;
    private List<Comment> comments;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemYear() {
        return itemYear;
    }

    public void setItemYear(String itemYear) {
        this.itemYear = itemYear;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getItemAuthor() {
        return itemAuthor;
    }

    public void setItemAuthor(String itemAuthor) {
        this.itemAuthor = itemAuthor;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPoster_url() {
        return poster_url;
    }

    public void setPoster_url(String poster_url) {
        this.poster_url = poster_url;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public Long getRecosCount() {
        return recosCount;
    }

    public void setRecosCount(Long recosCount) {
        this.recosCount = recosCount;
    }

    public Long getDirectedToId() {
        return directedToId;
    }

    public void setDirectedToId(Long directedToId) {
        this.directedToId = directedToId;
    }

    public String getDirectedToUsername() {
        return directedToUsername;
    }

    public void setDirectedToUsername(String directedToUsername) {
        this.directedToUsername = directedToUsername;
    }

    public long getLikes() {
        return likes;
    }

    public void setLikes(long likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }


    public List<String> getLikers() {
        return likers;
    }

    public void setLikers(List<String> likers) {
        this.likers = likers;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public long getGenreId() {
        return genreId;
    }

    public void setGenreId(long genreId) {
        this.genreId = genreId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }
}
