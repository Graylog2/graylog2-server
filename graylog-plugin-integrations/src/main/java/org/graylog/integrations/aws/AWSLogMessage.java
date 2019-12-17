package org.graylog.integrations.aws;

/**
 * A helper class that supports the ability to detect the type of AWS log message.
 */
public class AWSLogMessage {

    private String logMessage;

    public AWSLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }

    /**
     * Detects the type of log message.
     *
     * @param compressed Indicates if the payload is compressed and probably from CloudWatch.
     * @return A {@code Type} indicating the which kind of log message has been detected.
     */
    public AWSMessageType detectLogMessageType(boolean compressed) {

        // Compressed messages are always from CloudWatch.
        if (compressed) {
            if (isFlowLog()) {
                return AWSMessageType.KINESIS_CLOUDWATCH_FLOW_LOGS;
            } else {
                return AWSMessageType.KINESIS_CLOUDWATCH_RAW;
            }
        }

        return AWSMessageType.KINESIS_RAW;
    }

    /**
     * Flow logs are space-delimited messages. See https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html
     * <p>
     * Sample: 2 123456789010 eni-abc123de 172.31.16.139 172.31.16.21 20641 22 6 20 4249 1418530010 1418530070 ACCEPT OK
     * <p>
     * Match a message with exactly 13 spaces and either the word ACCEPT or REJECT.
     * Use simple if checks instead of regex to keep this simple. Performance should not be a concern, since
     * this is only called once during the healthcheck.
     *
     * @return true if message is a flow log.
     */
    public boolean isFlowLog() {

        // Though unlikely, the message could be null.
        if (logMessage == null) {
            return false;
        }

        boolean hasAction = logMessage.contains("ACCEPT") || logMessage.contains("REJECT");
        long spaceCount = logMessage.chars().filter(Character::isSpaceChar).count();

        return hasAction && spaceCount == 13;
    }
}