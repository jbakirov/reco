package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Baka on 29.09.2015.
 */
public class EmailValidator {
    private Pattern pattern;
    private Matcher matcher;

    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public EmailValidator(){
        pattern = Pattern.compile(EMAIL_PATTERN);
    }

    public boolean validate (final String email){
        matcher = pattern.matcher(email);
        return matcher.matches();
    }

}
