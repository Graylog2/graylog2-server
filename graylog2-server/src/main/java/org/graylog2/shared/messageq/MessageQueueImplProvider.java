package org.graylog2.shared.messageq;

import org.graylog2.plugin.BaseConfiguration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class MessageQueueImplProvider<T> implements Provider<T> {

    private final T impl;

    @Inject
    public MessageQueueImplProvider(BaseConfiguration config, Map<String, Provider<T>> providerMap) {
        final String journalMode = config.getMessageJournalMode();
        if (! providerMap.containsKey(journalMode)) {
            throw new IllegalArgumentException(
                    "Invalid journal mode [" + journalMode + "]. Valid journal modes are: " + providerMap.keySet() +
                            ". Please adjust the setting for \"message_journal_mode\" in your configuration.");
        }
        impl = providerMap.get(config.getMessageJournalMode()).get();
    }

    @Override
    public T get() {
        return impl;
    }
}
