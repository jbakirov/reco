package models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Created by Baka on 04.09.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Comment {

    private Long uid;
    private String username;
    private String profilePic;
    private Long postId;
    private Long cid;
    private String comment;
    private String timestamp;

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
}
