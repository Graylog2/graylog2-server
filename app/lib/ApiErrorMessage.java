package lib;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class ApiErrorMessage {
    public String type;
    public String message;
}
