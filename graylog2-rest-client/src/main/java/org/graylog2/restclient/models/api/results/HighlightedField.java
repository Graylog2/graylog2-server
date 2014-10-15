/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
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
