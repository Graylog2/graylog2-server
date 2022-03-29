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
package org.graylog.plugins.views.search.validation;

import org.apache.lucene.queryparser.classic.CharStream;

import java.io.IOException;

public class LineCountingCharStream implements CharStream {

    private final CharStream delegate;
    private final TokenLineCounter tokenLineCounter;

    public LineCountingCharStream(CharStream delegate) {
        this.delegate = delegate;
        this.tokenLineCounter = new TokenLineCounter(delegate);
    }

    @Override
    public char BeginToken() throws IOException {
        try {
            final char oneChar = delegate.BeginToken();
            tokenLineCounter.beginToken(oneChar);
            return oneChar;
        } catch (IOException e) {
            // in case of error we won't be able to detect current position from our own counting (won't be called),
            // we have to increment here manually
            if(e.getMessage().equals("read past eof")) {
                tokenLineCounter.incrementTokenStartPositionOnError();
            }
            throw e;
        }
    }

    @Override
    public char readChar() throws IOException {
        final char oneChar = delegate.readChar();
        tokenLineCounter.processChar(oneChar);
        return oneChar;
    }

    @Deprecated
    @Override
    public int getColumn() {
        return delegate.getColumn();
    }

    @Deprecated
    @Override
    public int getLine() {
        return delegate.getLine();
    }

    @Override
    public int getBeginLine() {
        return tokenLineCounter.getBeginLine();
    }

    @Override
    public int getBeginColumn() {
        return tokenLineCounter.getBeginColumn();
    }

    @Override
    public int getEndLine() {
        return tokenLineCounter.getEndLine();
    }

    @Override
    public int getEndColumn() {
        return tokenLineCounter.getEndColumn();
    }

    @Override
    public void backup(int amount) {
        delegate.backup(amount);
    }

    @Override
    public String GetImage() {
        return delegate.GetImage();
    }

    @Override
    public char[] GetSuffix(int len) {
        return delegate.GetSuffix(len);
    }

    @Override
    public void Done() {
        delegate.Done();
    }
}
