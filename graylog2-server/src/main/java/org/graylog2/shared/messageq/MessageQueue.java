package org.graylog2.shared.messageq;

import com.google.common.util.concurrent.Service;

import javax.annotation.Nullable;
import java.util.List;

public interface MessageQueue extends Service {
    default Entry createEntry(byte[] id, byte[] value) {
        return createEntry(id, null, value, 0);
    }

    default Entry createEntry(byte[] id, byte[] value, long timestamp) {
        return createEntry(id, null, value, timestamp);
    }

    Entry createEntry(byte[] id, @Nullable byte[] key, byte[] value, long timestamp);

    interface Entry {
        /**
         * The journal entry ID.
         * @return the ID value
         */
        byte[] id();

        /**
         * The journal entry key. This is supposed to be the shard key for journal implementations that support it.
         * @return the key value
         */
        @Nullable
        byte[] key();

        /**
         * The journal entry value.
         * @return the vale
         */
        byte[] value();

        /**
         * This is the event time in milliseconds of the entry.
         * @return the event time in milliseconds
         */
        long timestamp();
    }

    interface Envelope extends Iterable<Entry> {
        // TODO: Do we really want this or would a simple Iterable/Iterator implementation enough? That would allow
        //       us to stream the entries instead of loading all of them into memory.
        List<Entry> entries();

        void commitAll() throws MessageQueueException;

        void commit(Entry entry) throws MessageQueueException;
    }
}
