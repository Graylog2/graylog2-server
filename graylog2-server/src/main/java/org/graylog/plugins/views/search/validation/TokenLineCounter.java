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

public class TokenLineCounter {

    private final CharStream delegate;

    /**
     * Track already processed position
     */
    private int positionPointer;

    private int tokenBeginsAtLine;
    private int tokenBeginsAtPosition;

    /**
     * Current lines count detected
     */
    private int lineCounter = 1;
    private int currentLineStartsAtPosition;


    public TokenLineCounter(CharStream delegate) {
        this.delegate = delegate;
    }

    public void beginToken(char oneChar) {
        this.tokenBeginsAtLine = this.lineCounter;
        this.tokenBeginsAtPosition = delegate.getBeginColumn();
        this.processChar(oneChar);
    }

    public void processChar(char oneChar) {
        final int currentCharPosition = delegate.getEndColumn();
        if (currentCharPosition > this.positionPointer) { // to prevent duplicated calls
            this.positionPointer = delegate.getEndColumn();
            if (oneChar == '\n') {
                this.lineCounter++;
                this.currentLineStartsAtPosition = this.positionPointer;
            }
        }
    }

    public int getBeginLine() {
        return tokenBeginsAtLine;
    }

    public int getBeginColumn() {
        return this.tokenBeginsAtPosition - this.currentLineStartsAtPosition;
    }

    public int getEndLine() {
        return lineCounter;
    }

    public int getEndColumn() {
        return delegate.getEndColumn() - this.currentLineStartsAtPosition;
    }

    public void incrementTokenStartPositionOnError() {
        this.tokenBeginsAtPosition++;
        if(this.tokenBeginsAtLine != this.lineCounter) {
            this.tokenBeginsAtLine = this.lineCounter;
        }
    }
}
