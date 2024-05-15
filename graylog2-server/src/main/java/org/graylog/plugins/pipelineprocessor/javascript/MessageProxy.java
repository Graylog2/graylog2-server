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
package org.graylog.plugins.pipelineprocessor.javascript;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graylog2.plugin.Message;

import java.util.List;

public class MessageProxy implements ProxyObject {

    private final Message delegate;

    public MessageProxy(Message delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object getMember(String key) {
        // TODO; should we make sure to return an immutable copy?
        return delegate.getField(key);
    }

    @Override
    public Object getMemberKeys() {
        return ProxyArray.fromList(List.of(delegate.getFieldNames()));
    }

    @Override
    public boolean hasMember(String key) {
        return delegate.hasField(key);
    }

    @Override
    public void putMember(String key, Value value) {
        // TODO: allow adding directly to the message, without having to use the set_field function.
        //   We'll probably have to take care of type conversions, like in the PipelineFunctionProxy
        throw new UnsupportedOperationException("putMember() not supported at the moment.");
    }

    @Override
    public boolean removeMember(String key) {
        final int numberOfFieldsBeforeRemoval = delegate.getFieldCount();
        delegate.removeField(key);
        return delegate.getFieldCount() < numberOfFieldsBeforeRemoval;
    }

    public Message getDelegate() {
        return delegate;
    }
}
