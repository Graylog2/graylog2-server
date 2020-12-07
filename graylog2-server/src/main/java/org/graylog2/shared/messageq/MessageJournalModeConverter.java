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

import com.github.joschi.jadconfig.Converter;
import org.apache.directory.api.util.Strings;

public class MessageJournalModeConverter implements Converter<MessageJournalMode> {
    @Override
    public MessageJournalMode convertFrom(String value) {
        return MessageJournalMode.valueOf(Strings.upperCase(value));
    }

    @Override
    public String convertTo(MessageJournalMode value) {
        return value.name();
    }
}
