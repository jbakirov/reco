package models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Created by Baka on 06.11.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponse {

    private Map<String, String> question;
    private List<Map<String, String>> comments;

    public Map<String, String> getQuestion() {
        return question;
    }

    public void setQuestion(Map<String, String> question) {
        this.question = question;
    }

    public List<Map<String, String>> getComments() {
        return comments;
    }

    public void setComments(List<Map<String, String>> comments) {
        this.comments = comments;
    }
}
