package models.api.results;

import org.graylog2.restclient.models.api.responses;

import java.util.ArrayList;
import java.util.List;

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

    public List<FieldChunk> getChunks() {
        final String message = (String) messageResult.getFields().get(field);
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
