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
package org.graylog2.utilities;

public class InterruptibleCharSequence implements CharSequence {
    private CharSequence inner;

    public InterruptibleCharSequence(CharSequence inner) {
        super();
        this.inner = inner;
    }

    @Override
    public char charAt(int index) {
        if (Thread.interrupted()) { // clears flag if set
            throw new RuntimeException(new InterruptedException());
        }
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new InterruptibleCharSequence(inner.subSequence(start, end));
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
