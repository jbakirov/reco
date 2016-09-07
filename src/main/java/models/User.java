package models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Baka on 26.07.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private long id;
    private String user_name;
    private String first_name;
    private String second_name;
    private String user_email;
    private Integer phone_number;
    private String registration_date;
    private String password;
    private Integer rec_count;
    private Integer questions_count;
    private Integer total_comments;
    private Integer reputation;
    private Integer follower_count;
    private Integer following_count;
    private String profilePic;
    private String action;
    private Boolean isFollowing;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getSecond_name() {
        return second_name;
    }

    public void setSecond_name(String second_name) {
        this.second_name = second_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public Integer getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(Integer phone_number) {
        this.phone_number = phone_number;
    }

    public String getRegistration_date() {
        return registration_date;
    }

    public void setRegistration_date(String registration_date) {
        this.registration_date = registration_date;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRec_count() {
        return rec_count;
    }

    public void setRec_count(Integer rec_count) {
        this.rec_count = rec_count;
    }

    public Integer getQuestions_count() {
        return questions_count;
    }

    public void setQuestions_count(Integer questions_count) {
        this.questions_count = questions_count;
    }

    public Integer getTotal_comments() {
        return total_comments;
    }

    public void setTotal_comments(Integer total_comments) {
        this.total_comments = total_comments;
    }

    public Integer getReputation() {
        return reputation;
    }

    public void setReputation(Integer reputation) {
        this.reputation = reputation;
    }

    public Integer getFollower_count() {
        return follower_count;
    }

    public void setFollower_count(Integer follower_count) {
        this.follower_count = follower_count;
    }

    public Integer getFollowing_count() {
        return following_count;
    }

    public void setFollowing_count(Integer following_count) {
        this.following_count = following_count;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
