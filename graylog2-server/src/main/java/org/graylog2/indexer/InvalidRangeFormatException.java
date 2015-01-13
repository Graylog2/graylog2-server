package org.graylog2.indexer;

public class InvalidRangeFormatException extends RuntimeException {
    public InvalidRangeFormatException() {
        super("Invalid timerange parameters provided");
    }

    public InvalidRangeFormatException(String message) {
        super(message);
    }

    public InvalidRangeFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRangeFormatException(Throwable cause) {
        super("Invalid timerange parameters provided", cause);
    }
}
