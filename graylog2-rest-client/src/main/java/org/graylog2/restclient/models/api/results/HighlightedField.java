/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.api.results;

import org.graylog2.restclient.models.api.responses.HighlightRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HighlightedField {
    private final MessageResult messageResult;
    private final String field;

    public static class FieldChunk {
        private final String string;
        private final boolean escape;

        public FieldChunk(final String string, final boolean escape) {
            this.string = string;
            this.escape = escape;
        }

        public boolean avoidHtmlEscape() {
            return !escape;
        }

        public String getString() {
            return string;
        }
    }

    public HighlightedField(final MessageResult messageResult, final String field) {
        this.messageResult = messageResult;
        this.field = field;
    }

    public List<FieldChunk> getChunks(final Map<String, Object> fields) {
        final String message = (String) fields.get(field);
        final List<FieldChunk> list = new ArrayList<>();
        final List<HighlightRange> rangesList = messageResult.getHighlightRanges().get(field);

        if (rangesList == null) {
            throw new RuntimeException("No highlight ranges for field: " + field);
        }

        // Current position in the message string.
        int position = 0;

        for (int idx = 0; idx < rangesList.size(); idx++) {
            final HighlightRange range = rangesList.get(idx);

            // Part before the next highlighted range of the message. (avoid empty chunks)
            if (position != range.getStart()) {
                list.add(new FieldChunk(message.substring(position, range.getStart()), true));
            }

            // The highlighted range of the message. Only allow the highlighting tags to be unescaped. The highlighted
            // part can contain HTML elements so that should be escaped.
            list.add(new FieldChunk("<span class=\"result-highlight\">",false));
            list.add(new FieldChunk(message.substring(range.getStart(), range.getStart() + range.getLength()), true));
            list.add(new FieldChunk("</span>", false));

            if ((idx + 1) < rangesList.size() && rangesList.get(idx + 1) != null) {
                // If there is another highlight range, add the part between the end of the current range and the start
                // of the next range.
                final HighlightRange nextRange = rangesList.get(idx + 1);

                list.add(new FieldChunk(message.substring(range.getStart() + range.getLength(), nextRange.getStart()), true));

                position = nextRange.getStart();
            } else {
                // If this range is the last, just add the rest of the message.
                list.add(new FieldChunk(message.substring(range.getStart() + range.getLength()), true));

                position = range.getStart() + range.getLength();
            }

        }

        return list;
    }
}
