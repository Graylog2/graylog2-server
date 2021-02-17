/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.messageq;

import org.graylog2.plugin.BaseConfiguration;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class MessageQueueImplProvider<T> implements Provider<T> {

    private final Provider<T> provider;

    @Inject
    public MessageQueueImplProvider(BaseConfiguration config, Map<String, Provider<T>> providerMap) {
        final String journalMode = config.getMessageJournalMode();
        if (! providerMap.containsKey(journalMode)) {
            throw new IllegalArgumentException(
                    "Invalid journal mode [" + journalMode + "]. Valid journal modes are: " + providerMap.keySet() +
                            ". Please adjust the setting for \"message_journal_mode\" in your configuration.");
        }
        provider = providerMap.get(config.getMessageJournalMode());
    }

    @Override
    public T get() {
        return provider.get();
    }
}
