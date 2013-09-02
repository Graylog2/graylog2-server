package lib;

public class APIException extends Exception {

	private final int httpCode;
	private final String body;

	public APIException(int httpCode) {
        this(httpCode, null, (String)null);

    }

    public APIException(int httpCode, String msg) {
        this(httpCode, msg, (String) null);
    }

    public APIException(int httpCode, String msg, Throwable cause) {
        super(msg, cause);
        this.httpCode = httpCode;
        this.body = null;
    }


    public APIException(int httpCode, String msg, String body) {
		super(msg);
		this.httpCode = httpCode;
        this.body = body;
	}

    public String getBody() {
        return body;
    }

	public int getHttpCode() {
		return httpCode;
	}
	
}
