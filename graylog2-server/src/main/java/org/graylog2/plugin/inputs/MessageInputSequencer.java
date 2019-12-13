package org.graylog2.plugin.inputs;

import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Generates sequence numbers for a given millisecond timestamp.
 * <p>
 * It generates an increasing sequence number for each millisecond. The sequence number will start at 0 for a new
 * timestamp.
 * </p>
 * <p>
 * The sequence number is reset to 0 if the timestamp changes. Even if the timestamp value decreases. The user needs
 * to make sure that the timestamp value is only increasing to ensure correct sequence number generation.
 * </p>
 */
public class MessageInputSequencer {
    private final AtomicInteger sequenceNum;
    private final AtomicLong prevTimestamp;

    public MessageInputSequencer() {
        this(0, 0L);
    }

    @VisibleForTesting
    MessageInputSequencer(int initialSequenceNum, long initialTimestamp) {
        checkArgument(initialSequenceNum >= 0, "initialSequenceNum must be >= 0");
        checkArgument(initialTimestamp>= 0, "initialTimestamp must be >= 0");
        sequenceNum = new AtomicInteger(initialSequenceNum);
        prevTimestamp = new AtomicLong(initialTimestamp);
    }

    /**
     * Returns the next sequence number for the given timestamp in milliseconds. If the timestamp changes, the
     * sequence number will be reset to 0.
     * This also happens if the given timestamp is smaller than the previous one. (time goes backwards)
     * <p>
     * This method will never return a negative value and wraps around to 0 if {@link Integer#MAX_VALUE} is reached
     * for a millisecond.
     * </p>
     *
     * @param timestamp the timestamp in milliseconds
     * @return next sequence number
     */
    public int next(long timestamp) {
        checkArgument(timestamp >= 0, "timestamp must be >= 0");

        // We reset the sequence number when the timestamp changes. Event if the timestamp is lower than the previous
        // one. (which should usually not happen)
        if (timestamp != prevTimestamp.get()) {
            prevTimestamp.set(timestamp);
            sequenceNum.set(0);
        }
        final int nextSequenceNum = sequenceNum.getAndIncrement();
        if (nextSequenceNum < 0) {
            // Sequence number wrapped around so we reset to 0 because we don't want to return a negative number
            sequenceNum.set(0);
            return sequenceNum.getAndIncrement();
        }
        return nextSequenceNum;
    }
}
