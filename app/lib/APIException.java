package lib;

public class APIException extends Exception {

	private final int httpCode;
	
	public APIException(int httpCode) {
		super();
		this.httpCode = httpCode;
	}
	
	public APIException(int httpCode, String msg) {
		super(msg);
		this.httpCode = httpCode;
	}
	
	public int getHttpCode() {
		return httpCode;
	}
	
}
